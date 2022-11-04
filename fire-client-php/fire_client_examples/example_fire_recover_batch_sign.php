<html>
 <head>
  <title>Recuperar firma del lote de firmas</title>
 </head>
 <body>
 <?php 
	// Cargamos el componente distribuido de FIRe
	include '../fire_client.php';
	
	$appId = $_GET["appid"];		// Identificador de la aplicacion (dada de alta previamente en el sistema)
	$subjectId = $_GET["sid"];		// DNI de la persona
	$transactionId = $_GET["trid"];	// Identificador de la transaccion
	
	$fireClient = new FireClient($appId); // Identificador de la aplicacion (dada de alta previamente en el sistema)
	
	// Resultado de la primera firma
	$docId = "0001";
	try {
		// Funcion del API de FIRe para recuperar una firma de un lote
		$transactionResult = $fireClient->recoverBatchSign(
			$subjectId,			// DNI de la persona
			$transactionId,		// Identificador de transaccion recuperado en la operacion createBatch()
			$docId				// Identificador del documento
		);
		// Mostramos los datos obtenidos
		echo "<b>Firma del documento ", $docId, ":</b><br>", base64_encode($transactionResult->result), "<br>";
	}
	catch(Exception $e) {
		echo "<b>Error al recuperar el documento ", $docId, ":</b><br>Error ", $e->getCode(), ": ", $e->getMessage(), "<br>";   
	}

	// Resultado de la segunda firma
	$docId = "0002";
	try {
		$transactionResult = $fireClient->recoverBatchSign(
			$subjectId,			// DNI de la persona
			$transactionId,		// Identificador de transaccion recuperado en la operacion createBatch()
			$docId				// Identificador del documento
		);
		// Mostramos los datos obtenidos
		echo "<b>Firma del documento ", $docId, ":</b><br>" , base64_encode($transactionResult->result), "<br>";
	}
	catch(Exception $e) {
		echo "<b>Error al recuperar el documento ", $docId, ":</b><br>Error ", $e->getCode(), ": ", $e->getMessage(), "<br>";   
	}
	
	echo "<br><br><br><a href=\"example_fire_create_batch.php\">Volver >></a>";
 ?>
 </body>
</html>