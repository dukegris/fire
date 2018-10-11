/* Copyright (C) 2017 [Gobierno de Espana]
 * This file is part of FIRe.
 * FIRe is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 08/09/2017
 * You may contact the copyright holder at: soporte.afirma@correo.gob.es
 */
package es.gob.fire.server.services.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.signers.TriphaseData;
import es.gob.fire.server.connector.FIReConnector;
import es.gob.fire.server.connector.FIReConnectorFactoryException;
import es.gob.fire.server.connector.FIReConnectorUnknownUserException;
import es.gob.fire.server.connector.FIReSignatureException;
import es.gob.fire.server.document.FIReDocumentManager;
import es.gob.fire.server.services.AfirmaUpgrader;
import es.gob.fire.server.services.FIReTriHelper;
import es.gob.fire.server.services.HttpCustomErrors;
import es.gob.fire.server.services.RequestParameters;
import es.gob.fire.server.services.ServiceUtil;


/**
 * Manejador encargado de la composici&oacute;n de las firmas, su actualizaci&oacute;n
 * de ser preciso, y la devoluci&oacute;n al cliente.
 */
public class RecoverSignManager {

	private static final Logger LOGGER = Logger.getLogger(RecoverSignManager.class.getName());

	/**
	 * Finaliza un proceso de firma y devuelve el resultado del mismo.
	 * @param params Par&aacute;metros extra&iacute;dos de la petici&oacute;n.
	 * @param response Respuesta de la petici&oacute;n.
	 * @throws IOException Cuando se produce un error de lectura o env&iacute;o de datos.
	 */
	public static void recoverSignature(final RequestParameters params, final HttpServletResponse response)
			throws IOException {

		// Recogemos los parametros proporcionados en la peticion
		final String appId = params.getParameter(ServiceParams.HTTP_PARAM_APPLICATION_ID);
		final String transactionId = params.getParameter(ServiceParams.HTTP_PARAM_TRANSACTION_ID);
		final String subjectId = params.getParameter(ServiceParams.HTTP_PARAM_SUBJECT_ID);
		final String upgrade = params.getParameter(ServiceParams.HTTP_PARAM_UPGRADE);

        // Comprobamos que se hayan proporcionado los parametros indispensables
        if (transactionId == null || transactionId.isEmpty()) {
        	LOGGER.warning("No se ha proporcionado el ID de transaccion"); //$NON-NLS-1$
        	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

		LOGGER.info(String.format("App %1s: TrId %2s: Peticion bien formada", appId, transactionId)); //$NON-NLS-1$

        // Recuperamos el resto de parametros de la sesion
        FireSession session = SessionCollector.getFireSession(transactionId, subjectId, null, false, false);
        if (session == null) {
    		LOGGER.warning("La transaccion no se ha inicializado o ha caducado"); //$NON-NLS-1$
    		response.sendError(HttpCustomErrors.INVALID_TRANSACTION.getErrorCode());
    		return;
        }

        // Si la operacion anterior no fue el inicio de una firma, forzamos a que se recargue por si faltan datos
		if (SessionFlags.OP_PRE != session.getObject(ServiceParams.SESSION_PARAM_PREVIOUS_OPERATION)) {
			session = SessionCollector.getFireSession(transactionId, subjectId, null, false, true);
		}

        // Comprobamos que no se haya declarado ya un error, en cuyo caso, lo devolvemos
        if (session.containsAttribute(ServiceParams.SESSION_PARAM_ERROR_TYPE)) {
        	final String errType = session.getString(ServiceParams.SESSION_PARAM_ERROR_TYPE);
        	final String errMessage = session.getString(ServiceParams.SESSION_PARAM_ERROR_MESSAGE);
        	SessionCollector.removeSession(session);
        	LOGGER.warning("Ocurrio un error durante la operacion de firma: " + errMessage); //$NON-NLS-1$
        	sendResult(
        			response,
        			new TransactionResult(
        					TransactionResult.RESULT_TYPE_SIGN,
        					Integer.parseInt(errType),
        					errMessage));
        	return;
        }

        // Extraemos la configuracion de firma
        final String tdB64			= session.getString(ServiceParams.SESSION_PARAM_TRIPHASE_DATA); // 1
        final String certB64		= session.getString(ServiceParams.SESSION_PARAM_CERT); // 2
        final byte[] docId			= (byte[]) session.getObject(ServiceParams.SESSION_PARAM_DOC_ID);
        final String cop			= session.getString(ServiceParams.SESSION_PARAM_CRYPTO_OPERATION);
        final String algorithm		= session.getString(ServiceParams.SESSION_PARAM_ALGORITHM);
        final String format			= session.getString(ServiceParams.SESSION_PARAM_FORMAT);
        final String extraParamsB64	= session.getString(ServiceParams.SESSION_PARAM_EXTRA_PARAM);

        final String providerName	= session.getString(ServiceParams.SESSION_PARAM_CERT_ORIGIN);
        final String remoteTrId		= session.getString(ServiceParams.SESSION_PARAM_REMOTE_TRANSACTION_ID); // 3

        final TransactionConfig connConfig	=
        		(TransactionConfig) session.getObject(ServiceParams.SESSION_PARAM_CONNECTION_CONFIG);

        // En caso de haberse indicado un DocumentManager, lo recogemos
        final FIReDocumentManager docManager	= (FIReDocumentManager) session.getObject(ServiceParams.SESSION_PARAM_DOCUMENT_MANAGER);

        // Decodificamos el certificado
        final X509Certificate signerCert;
        try {
            signerCert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate( //$NON-NLS-1$
                new ByteArrayInputStream(Base64.decode(certB64, true))
            );
        }
        catch (final Exception e) {
        	LOGGER.severe("No se ha podido decodificar el certificado del firmante: " + e); //$NON-NLS-1$
        	SessionCollector.removeSession(session);
        	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No se ha podido decodificar el certificado proporcionado: " + e); //$NON-NLS-1$
        	return;
        }

        // Decodificamos la configuracion de firma
        Properties extraParams;
        try {
        	extraParams = ServiceUtil.base642Properties(extraParamsB64);
        }
        catch (final Exception e) {
        	LOGGER.severe("Parametros extra de configuracion de la firma mal formatos: " + e); //$NON-NLS-1$
        	SessionCollector.removeSession(session);
        	response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Parametros extra de configuracion de la firma mal formatos: " + e); //$NON-NLS-1$
        	return;
		}

        // En el caso de la firma con certificado local, tendremos ya la firma
        // completa y la podemos procesar y devolver
        if (ServiceParams.CERTIFICATE_ORIGIN_LOCAL.equals(providerName)) {

        	LOGGER.info(String.format("App %1s: TrId %2s: Se firmo con certificado local y ya se puede devolver la firma", appId, transactionId)); //$NON-NLS-1$

        	// Recuperamos la respuesta del temporal en el que lo almacenamos
        	byte[] signResult;
        	try {
        		signResult = TempFilesHelper.retrieveAndDeleteTempData(transactionId);
        	}
        	catch (final Exception e) {
        		LOGGER.warning("No se encuentra la firma generada: " + e); //$NON-NLS-1$
            	SessionCollector.removeSession(session);
        		response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT, "Ha caducado la sesion"); //$NON-NLS-1$
        		return;
        	}

        	LOGGER.info(String.format("App %1s: TrId %2s: Firma cargada", appId, transactionId)); //$NON-NLS-1$

        	// Se actualiza si esta definido el formato de actualizacion
        	if (upgrade != null && !upgrade.isEmpty()) {
        		LOGGER.info(String.format("App %1s: TrId %2s: Se actualiza la firma al formato %3s", appId, transactionId, upgrade)); //$NON-NLS-1$
        		try {
        			signResult = AfirmaUpgrader.upgradeSignature(signResult, upgrade);
        		}
        		catch (final Exception e) {
        			LOGGER.log(Level.SEVERE, "Error al actualizar la firma de la transaccion: " + transactionId, e); //$NON-NLS-1$
        			SessionCollector.removeSession(session);
        			response.sendError(HttpCustomErrors.UPGRADING_ERROR.getErrorCode());
        			return;
        		}
        	}

        	// Operamos con la firma tal como lo indique el DocumentManager
        	LOGGER.info(String.format("App %1s: TrId %2s: Se postprocesa la firma con el DocumentManager", appId, transactionId)); //$NON-NLS-1$
        	try {
        		signResult = docManager.storeDocument(docId, appId, signResult, signerCert, format, extraParams);
        	}
        	catch (final Exception e) {
        		LOGGER.log(Level.SEVERE, "Error al postprocesar con el FIReDocumentManager la firma del documento", e); //$NON-NLS-1$
        		SessionCollector.removeSession(session);
            	response.sendError(HttpCustomErrors.SAVING_ERROR.getErrorCode());
    			return;
        	}

        	// Guardamos la firma resultante para devolverla despues, ya que en un primer
        	// momento solo responderemos con el resultado de la operacion y no con la propia
        	// firma generada
        	LOGGER.info(String.format("App %1s: TrId %2s: Se almacena temporalmente el resultado de la operacion", appId, transactionId)); //$NON-NLS-1$
        	try {
        		TempFilesHelper.storeTempData(transactionId, signResult);
        	}
        	catch (final Exception e) {
        		LOGGER.log(Level.SEVERE, "Error al almacenar la firma despues de haberla completado", e); //$NON-NLS-1$
            	SessionCollector.removeSession(session);
            	response.sendError(HttpCustomErrors.SAVING_ERROR.getErrorCode());
    			return;
        	}

        	// Ya no necesitaremos de nuevo la sesion, asi que la eliminamos del pool
        	session.setAttribute(ServiceParams.SESSION_PARAM_PREVIOUS_OPERATION, SessionFlags.OP_RECOVER);
        	SessionCollector.commit(session);

        	LOGGER.info(String.format("App %1s: TrId %2s: Se devuelve la informacion del resultado de la operacion", appId, transactionId)); //$NON-NLS-1$

        	// Enviamos el resultado de la operacion (tipo de operacion y proveedor utilizado)
        	sendResult(response, buildResult(providerName));
        	return;
        }


        // En el caso de firma con un certificado en la nube, todavia tendremos que
        // componer la propia firma

    	LOGGER.info(String.format("App %1s: TrId %2s: Se firmo con el proveedor %3s y es necesario recuperar el PKCS#1 resultante y completar la firma", appId, transactionId, providerName)); //$NON-NLS-1$

        final byte[] data;
        try {
        	data = TempFilesHelper.retrieveAndDeleteTempData(transactionId);
        }
        catch (final Exception e) {
        	LOGGER.warning("No se encuentra la firma parcial generada: " + e); //$NON-NLS-1$
        	SessionCollector.removeSession(session);
        	response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT,
        			"Ha caducado la sesion" //$NON-NLS-1$
        			);
        	return;
		}

    	if (connConfig == null) {
    		LOGGER.warning("No se proporcionaron datos para la conexion con el backend"); //$NON-NLS-1$
        	response.sendError(HttpServletResponse.SC_BAD_REQUEST,
   					"No se proporcionaron datos para la conexion con el backend"); //$NON-NLS-1$
   			return;
    	}

    	LOGGER.info(String.format("App %1s: TrId %2s: Se solicita el PKCS#1 al proveedor %3s", appId, transactionId, providerName)); //$NON-NLS-1$

        // Obtenemos el conector con el backend ya configurado
        final FIReConnector connector;
        try {
    		connector = ProviderManager.initTransacction(providerName, connConfig.getProperties());
        }
        catch (final FIReConnectorFactoryException e) {
            LOGGER.log(Level.SEVERE, "Error en la configuracion del conector del servicio de custodia", e); //$NON-NLS-1$
            SessionCollector.removeSession(session);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        final Map<String, byte[]> ret;
        try {
            ret = connector.sign(remoteTrId);
        }
		catch(final FIReConnectorUnknownUserException e) {
			LOGGER.log(Level.WARNING, "El usuario no esta dado de alta en el sistema", e); //$NON-NLS-1$
            SessionCollector.removeSession(session);
			response.sendError(HttpCustomErrors.NO_USER.getErrorCode());
	        return;
		}
		catch(final Exception e) {
			LOGGER.log(Level.WARNING, "Error durante el proceso de firma", e); //$NON-NLS-1$
			SessionCollector.removeSession(session);
			response.sendError(HttpCustomErrors.SIGN_ERROR.getErrorCode());
			return;
		}

        final TriphaseData td;
        try {
        	td = TriphaseData.parser(Base64.decode(tdB64, true));
        }
        catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error de codificacion en los datos de firma trifasica proporcionados", e); //$NON-NLS-1$
            SessionCollector.removeSession(session);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Insertamos los PKCS#1 en la sesion trifasica
        final Set<String> keys = ret.keySet();
        for (final String key : keys) {
            FIReTriHelper.addPkcs1ToTriSign(ret.get(key), key, td);
        }

    	LOGGER.info(String.format("App %1s: TrId %2s: Se completa el proceso de firma", appId, transactionId)); //$NON-NLS-1$

        byte[] signResult;
        try {
            signResult = FIReTriHelper.getPostSign(
                    cop,
                    format,
                    algorithm,
                    extraParams,
                    signerCert,
                    data,
                    td
            );
        }
        catch (final FIReSignatureException e) {
            LOGGER.log(
        		Level.WARNING, "Error durante la postfirma. Verifique la operacion criptografica (" + //$NON-NLS-1$
        				cop + ") y el formato (" + format + ")", e //$NON-NLS-1$ //$NON-NLS-2$
            );
            SessionCollector.removeSession(session);
            response.sendError(HttpCustomErrors.POSTSIGN_ERROR.getErrorCode());
            return;
        }

		// Se actualiza si esta definido el formato de actualizacion
        if (upgrade != null && !upgrade.isEmpty()) {
        	LOGGER.info(String.format("App %1s: TrId %2s: Se actualiza la firma al formato %3s", appId, transactionId, upgrade)); //$NON-NLS-1$
        	try {
        		signResult = AfirmaUpgrader.upgradeSignature(signResult, upgrade);
        	}
        	catch (final Exception e) {
        		LOGGER.log(Level.WARNING, "Error al actualizar la firma de la transaccion: " + transactionId, e); //$NON-NLS-1$
        		response.sendError(HttpCustomErrors.UPGRADING_ERROR.getErrorCode());
        		return;
        	}
        }

        // Notificamos al conector que ha terminado la operacion para que libere recursos y
        // cierre la transaccion
        connector.endSign(remoteTrId);

        // Operamos con la firma tal como lo indique el DocumentManager
    	LOGGER.info(String.format("App %1s: TrId %2s: Se postprocesa la firma con el DocumentManager", appId, transactionId)); //$NON-NLS-1$
        try {
        	signResult = docManager.storeDocument(docId, appId, signResult, signerCert, format, extraParams);
        }
        catch (final Exception e) {
        	LOGGER.log(Level.SEVERE, "Error en el guardado de la firma del documento " + docId + //$NON-NLS-1$
        			" de la transaccion: " + transactionId, e); //$NON-NLS-1$
            SessionCollector.removeSession(session);
			response.sendError(HttpCustomErrors.SAVING_ERROR.getErrorCode());
			return;
		}

    	// Guardamos la firma resultante para devolverla despues, ya que en un primer
    	// momento solo responderemos con el resultado de la operacion y no con la propia
    	// firma generada
        LOGGER.info(String.format("App %1s: TrId %2s: Se almacena temporalmente el resultado de la operacion", appId, transactionId)); //$NON-NLS-1$
    	try {
    		TempFilesHelper.storeTempData(transactionId, signResult);
    	}
    	catch (final Exception e) {
    		LOGGER.log(Level.SEVERE, "Error al almacenar la firma despues de haberla completado", e); //$NON-NLS-1$
        	SessionCollector.removeSession(session);
        	response.sendError(HttpCustomErrors.SAVING_ERROR.getErrorCode());
			return;
    	}

    	LOGGER.info(String.format("App %1s: TrId %2s: Se devuelve la informacion del resultado de la operacion", appId, transactionId)); //$NON-NLS-1$

    	// Enviamos el resultado de la operacion (tipo de operacion y proveedor utilizado)
        sendResult(response, buildResult(providerName));
	}

	private static TransactionResult buildResult(final String providerName) {
		return new TransactionResult(TransactionResult.RESULT_TYPE_SIGN, providerName);
	}

	private static void sendResult(final HttpServletResponse response, final TransactionResult result) throws IOException {
		// El servicio devuelve el resultado de la operacion de firma.
        final OutputStream output = ((ServletResponse) response).getOutputStream();
        output.write(result.encodeResult());
        output.flush();
        output.close();
	}
}
