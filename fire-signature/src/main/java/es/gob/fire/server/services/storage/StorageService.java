/* Copyright (C) 2017 [Gobierno de Espana]
 * This file is part of FIRe.
 * FIRe is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 08/09/2017
 * You may contact the copyright holder at: soporte.afirma@correo.gob.es
 */
package es.gob.fire.server.services.storage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import es.gob.fire.server.services.LogUtils;
import es.gob.fire.server.services.RequestParameters;
import es.gob.fire.server.services.internal.TempDocumentsManager;
import es.gob.fire.signature.ConfigManager;


/** Servicio de almacenamiento temporal de firmas. &Uacute;til para servir de intermediario en comunicaci&oacute;n
 * entre JavaScript y el cliente Afirma.
 * @author Tom&aacute;s Garc&iacute;a-;er&aacute;s. */
public final class StorageService extends HttpServlet {

	private static final long serialVersionUID = -3272368448371213403L;

	/** Codificaci&oacute;n de texto. */
	private static final String DEFAULT_ENCODING = "utf-8"; //$NON-NLS-1$

	/** Log para registrar las acciones del servicio. */
	private static final Logger LOGGER = Logger.getLogger(StorageService.class.getName());

	/** Nombre del par&aacute;metro con la operaci&oacute;n realizada. */
	private static final String PARAMETER_NAME_OPERATION = "op"; //$NON-NLS-1$

	/** Nombre del par&aacute;metro con el identificador del fichero temporal. */
	private static final String PARAMETER_NAME_ID = "id"; //$NON-NLS-1$

	/** Nombre del par&aacute;metro con la versi&oacute;n de la sintaxis de petici&oacute;n utilizada. */
	private static final String PARAMETER_NAME_SYNTAX_VERSION = "v"; //$NON-NLS-1$

	/** Nombre del par&aacute;metro con los datos a firmar. */
	private static final String PARAMETER_NAME_DATA = "dat"; //$NON-NLS-1$

	private static final String OPERATION_STORE = "put"; //$NON-NLS-1$
	private static final String SUCCESS = "OK"; //$NON-NLS-1$

	private static final boolean HIGH_AVAILABILITY_ENABLED;

	static {
		final String sessionsDao = ConfigManager.getSessionsDao();
		HIGH_AVAILABILITY_ENABLED = sessionsDao != null && !sessionsDao.trim().isEmpty();
	}

	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		LOGGER.fine(" == INICIO GUARDADO == "); //$NON-NLS-1$

		// Leemos la entrada
		final RequestParameters params = RequestParameters.extractParameters(request);

		final String operation = params.get(PARAMETER_NAME_OPERATION);
		final String syntaxVersion = params.get(PARAMETER_NAME_SYNTAX_VERSION);
		final String id = params.get(PARAMETER_NAME_ID);
		response.setHeader("Access-Control-Allow-Origin", "*"); //$NON-NLS-1$ //$NON-NLS-2$
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); //$NON-NLS-1$ //$NON-NLS-2$
		response.setContentType("text/plain"); //$NON-NLS-1$
		response.setCharacterEncoding("utf-8"); //$NON-NLS-1$

		final PrintWriter out = response.getWriter();
		if (operation == null) {
			LOGGER.warning(ErrorManager.genError(ErrorManager.ERROR_MISSING_OPERATION_NAME));
			out.println(ErrorManager.genError(ErrorManager.ERROR_MISSING_OPERATION_NAME));
			out.flush();
			return;
		}
		if (syntaxVersion == null) {
			LOGGER.warning(ErrorManager.genError(ErrorManager.ERROR_MISSING_SYNTAX_VERSION));
			out.println(ErrorManager.genError(ErrorManager.ERROR_MISSING_SYNTAX_VERSION));
			out.flush();
			return;
		}
		if (id == null) {
			LOGGER.warning(ErrorManager.genError(ErrorManager.ERROR_MISSING_DATA_ID));
			out.println(ErrorManager.genError(ErrorManager.ERROR_MISSING_DATA_ID));
			out.flush();
			return;
		}

		storeSign(out, id, operation, params);

		LOGGER.fine("== FIN DEL GUARDADO =="); //$NON-NLS-1$
	}

	/** Almacena una firma en servidor.
	 * @param out Flujo de datos para la respuesta a la petici&oacute;n.
	 * @param id Identificador de los datos a guardar.
	 * @param operation Operacion solicitada. Debe ser #StorageService.OPERATION_STORE.
	 * @param params Par&aacute;metros de la petici&oacute;n.
	 * @throws IOException Cuando ocurre un error al general la respuesta. */
	private static void storeSign(final PrintWriter out, final String id, final String operation, final Map<String, String> params) throws IOException {

		LOGGER.info("Se solicita guardar un documento con el identificador: " + LogUtils.cleanText(id)); //$NON-NLS-1$

		// Si no se indican los datos, se transmite el error en texto plano a traves del fichero generado
		String dataText = URLDecoder.decode(params.get(PARAMETER_NAME_DATA), DEFAULT_ENCODING);
		if (dataText == null) {
			LOGGER.warning(ErrorManager.genError(ErrorManager.ERROR_MISSING_DATA));
			dataText = ErrorManager.genError(ErrorManager.ERROR_MISSING_DATA);
		}
		if (!StorageService.OPERATION_STORE.equalsIgnoreCase(operation)) {
			LOGGER.warning(ErrorManager.genError(ErrorManager.ERROR_UNSUPPORTED_OPERATION_NAME));
			dataText = ErrorManager.genError(ErrorManager.ERROR_UNSUPPORTED_OPERATION_NAME);
		}

		final byte[] data = dataText.getBytes();

		// Tratamos de guardar los datos en la cache en memoria
		ClienteAfirmaCache.saveData(id, data);




		//XXX: Borrar - INICIO
		LOGGER.info(" ==== Se guardo en memoria el documento con identificador: " + LogUtils.cleanText(id));
		//XXX: Borrar - FIN




		// Si estamos en modo alta disponibilidad, tambien almacenamos los datos en el almacen comun
		if (HIGH_AVAILABILITY_ENABLED) {
			try {
				TempDocumentsManager.storeDocument(id, dataText.getBytes(), true);
			} catch (final IOException e) {
				LOGGER.log(Level.SEVERE, "Error al guardar el temporal para la comunicacion con el Cliente @firma", e); //$NON-NLS-1$
				out.println(ErrorManager.genError(ErrorManager.ERROR_COMMUNICATING_WITH_WEB));
				return;
			}



			//XXX: Borrar - INICIO
			LOGGER.info(" ==== Se guardo en almacenamiento compartido el documento con identificador: " + LogUtils.cleanText(id));
			//XXX: Borrar - FIN



		}

		out.print(SUCCESS);
	}
}