<%@page import="es.gob.fire.client.GracePeriodInfo"%>
<%@page import="java.security.cert.CertificateEncodingException"%>
<%@page import="es.gob.fire.client.TransactionResult"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="es.gob.fire.test.webapp.Base64"%>
<%@page import="es.gob.fire.test.webapp.SignHelper"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	if (session.getAttribute("user") == null) { //$NON-NLS-1$
		response.sendRedirect("Login.jsp"); //$NON-NLS-1$
		return;
	}
%>

<!DOCTYPE html>
<html>
	<head>
		<meta charset="UTF-8">
		<title>Prueba FIRe</title>
		<link rel="shortcut icon" href="img/cert.png">
		<link rel="stylesheet" href="styles/styles.css"/>
		<script type="text/javascript">
			function b64toBlob (b64Data, contentType='', sliceSize=512) {
			  const byteCharacters = atob(b64Data);
			  const byteArrays = [];

			  for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
			    const slice = byteCharacters.slice(offset, offset + sliceSize);

			    const byteNumbers = new Array(slice.length);
			    for (let i = 0; i < slice.length; i++) {
			      byteNumbers[i] = slice.charCodeAt(i);
			    }

			    const byteArray = new Uint8Array(byteNumbers);
			    byteArrays.push(byteArray);
			  }

			  return new Blob(byteArrays, {type: contentType});
			}
		
		</script>
	</head>
	<body style=" font-weight: 300;">

		<%
		    TransactionResult result;
		    try {
		    	result = SignHelper.recoverSignResult(request);
		    }
		    catch (Exception e) {
				LoggerFactory.getLogger("es.gob.fire.test.webapp").error( //$NON-NLS-1$
						"Error durante la operacion de recuperacion de firma: {}", e.toString()); //$NON-NLS-1$
		    	response.sendRedirect("ErrorPage.jsp?msg=" + URLEncoder.encode(e.getMessage(), "utf-8")); //$NON-NLS-1$ //$NON-NLS-2$
		    	return;
		    }

		    final byte[] signature = result.getResult();
		    final GracePeriodInfo gracePeriod = result.getGracePeriod();
		    
		    final String resultMsg = signature != null ?
		    		Base64.encode(signature) :
		    		(gracePeriod != null ?
		    				("ID Periodo de gracia: " + gracePeriod.getResponseId() + //$NON-NLS-1$
		    					"\nFecha estimada: " + gracePeriod.getResolutionDate()) : //$NON-NLS-1$
		    				"No se ha obtenido resultado"); //$NON-NLS-1$
		    
		    LoggerFactory.getLogger("es.gob.fire.test.webapp").info( //$NON-NLS-1$
		    		"Estado: " + result.getState()); //$NON-NLS-1$
		    LoggerFactory.getLogger("es.gob.fire.test.webapp").info( //$NON-NLS-1$
		    		"Nombre de proveedor: " + result.getProviderName()); //$NON-NLS-1$
			LoggerFactory.getLogger("es.gob.fire.test.webapp").info( //$NON-NLS-1$
				    "Formato de actualizacion: " + result.getUpgradeFormat()); //$NON-NLS-1$
		    try {
		    	 LoggerFactory.getLogger("es.gob.fire.test.webapp").info( //$NON-NLS-1$
		    			 "Certificado de firma: " + (result.getSigningCert() != null ? //$NON-NLS-1$
							Base64.encode(result.getSigningCert().getEncoded()) : null));
			} catch (final CertificateEncodingException e) {
				LoggerFactory.getLogger("es.gob.fire.test.webapp").error( //$NON-NLS-1$
						"No se pudo decodificar el certificado de firma: " + e); //$NON-NLS-1$
			}
			
			// Identificamos si el resultado es demasiado grande, en cuyo caso no lo mostraremos en Base 64 
			final boolean dataTooLarge = resultMsg.length() > 1024 * 1024; // 1 Mb
			
			// Definimos la extension para la descarga de la firma segun el formato
			String ext = null;
			String signFormat = (String) session.getAttribute("format"); //$NON-NLS-1$
			if (signFormat != null) {
				switch (signFormat) {
					case "CAdES":
						ext = ".csig";
						break;
					case "XAdES":
						ext = ".xsig";
						break;
					case "PAdES":
						ext = ".pdf";
						break;
					case "FacturaE":
						ext = ".xml";
						break;
					case "CAdES-ASiC-S":
					case "XAdES-ASiC-S":
						ext = ".asics";
						break;
					default:
						ext = "";
				}
			}
		%>

		<!-- Barra de navegacion -->
		<div id="menubar">
			<div id="bar-txt"><b>Prueba FIRe</b></div>
		</div>

		<!-- contenido -->
		<div id="sign-container">
			<h1 style="color:#303030;">OBTENCI&Oacute;N DE LA FIRMA</h1>

			<div style="display:inline-block;"></div>

			<div style="margin-top: 10px; text-align: left; ">
				<% if (!dataTooLarge) { %>
					<label for="datos-firma">Resultado: </label><br><br>
					<textarea id="datos-firma" rows="10" cols="150" name="sign-data"><%= resultMsg %></textarea><br>
				<% } else { %>
					<span>La firma generada es demasiado grande para mostrarla. Pulse el siguiente enlace para descargar:</span>
				<% } %>
				<a id="download_link" download="firma<%= ext %>" href="" style="display: none">Descargar fichero</a>
			</div>

			<form method="POST" action="Login.jsp">
				<div style="margin-top:30px;text-align: center; ">
					<label for="submit-btn">Pulse el bot&oacute;n para realizar una nueva firma</label><br><br>
					<input  id="submit-btn"  type="submit" value="NUEVA FIRMA">
				</div>
			</form>
		</div>
	
	</body>
	
	<%    
		if (signature != null) {
	%>
			<script type="text/javascript">
				var blob = b64toBlob("<%= resultMsg %>");
				
				var url = window.URL.createObjectURL(blob);
		
				document.getElementById('download_link').href = url;
				document.getElementById('download_link').style = "display: block";
			</script>
	<%
		}
	%>
</html>