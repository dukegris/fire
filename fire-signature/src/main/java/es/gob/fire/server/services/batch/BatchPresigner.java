/* Copyright (C) 2017 [Gobierno de Espana]
 * This file is part of FIRe.
 * FIRe is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 08/09/2017
 * You may contact the copyright holder at: soporte.afirma@correo.gob.es
 */
package es.gob.fire.server.services.batch;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/** Realiza la primera fase de un proceso de firma por lote.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class BatchPresigner extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(BatchPresigner.class.getName());

	private static final String BATCH_JSON_PARAM = "json"; //$NON-NLS-1$
	private static final String BATCH_CRT_PARAM = "certs"; //$NON-NLS-1$

	private static final String CONFIG_PARAM_ALLOW_ORIGIN = "Access-Control-Allow-Origin"; //$NON-NLS-1$

	/** Or&iacute;genes permitidos por defecto desde los que se pueden realizar peticiones al servicio. */
	private static final String ALL_ORIGINS_ALLOWED = "*"; //$NON-NLS-1$


	/** Realiza la primera fase de un proceso de firma por lote.
	 * Debe recibir la definici&oacute;n del lote en un JSON (<a href="../doc-files/batch-scheme.html">descripci&oacute;n
	 * del formato</a>) convertido completamente
	 * en Base64 y la cadena de certificados del firmante, convertidos a Base64 (puede ser
	 * <i>URL Safe</i>) y separados por punto y coma (<code>;</code>).
	 * Devuelve un JSON de sesi&oacute;n trif&aacute;sica.
	 * @see HttpServlet#service(HttpServletRequest request, HttpServletResponse response) */
	@Override
	protected void service(final HttpServletRequest request,
			               final HttpServletResponse response) throws ServletException,
			                                                          IOException {
		final String json = request.getParameter(BATCH_JSON_PARAM);
		if (json == null) {
			LOGGER.severe("No se ha recibido una definicion de lote en el parametro " + BATCH_JSON_PARAM); //$NON-NLS-1$
			response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"No se ha recibido una definicion de lote en el parametro " + BATCH_JSON_PARAM //$NON-NLS-1$
			);
			return;
		}

		// No se guardaran los resultados en cache
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); //$NON-NLS-1$ //$NON-NLS-2$

		final SignBatch batch;
		try {
			final byte[] batchConfig = BatchServerUtil.getSignBatchConfig(json);
			batch = new SignBatchSerial(batchConfig);
		}
		catch(final Exception e) {
			LOGGER.severe("La definicion de lote es invalida: " + e); //$NON-NLS-1$
			response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"La definicion de lote es invalida: " + e //$NON-NLS-1$
			);
			return;
		}

		final String certListUrlSafeBase64 = request.getParameter(BATCH_CRT_PARAM);
		if (certListUrlSafeBase64 == null) {
			LOGGER.severe("No se ha recibido la cadena de certificados del firmante en el parametro " + BATCH_CRT_PARAM); //$NON-NLS-1$
			response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"No se ha recibido la cadena de certificados del firmante en el parametro " + BATCH_CRT_PARAM //$NON-NLS-1$
			);
			return;
		}

		final X509Certificate[] certs;
		try {
			certs = BatchServerUtil.getCertificates(certListUrlSafeBase64);
		}
		catch (final Exception e) {
			LOGGER.severe("La cadena de certificados del firmante es invalida: " + e); //$NON-NLS-1$
			response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"La cadena de certificados del firmante es invalida: " + e //$NON-NLS-1$
			);
			return;
		}

		final JsonObject pre;
		try {
			pre = batch.doPreBatch(certs);
		}
		catch(final Exception e) {
			LOGGER.log(Level.SEVERE, "Error en el preproceso del lote: " + e, e); //$NON-NLS-1$
			response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Error en el preproceso del lote: " + e //$NON-NLS-1$
			);
			return;
		}

		response.setHeader(CONFIG_PARAM_ALLOW_ORIGIN, ALL_ORIGINS_ALLOWED);
		response.setContentType("application/json"); //$NON-NLS-1$
		try (final PrintWriter writer = response.getWriter()) {
			writer.write(pre.toString());
			writer.flush();
		}
	}

}
