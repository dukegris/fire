-- ********************************************************
-- *** Tablas para el guardado de sesiones y documentos ***
-- *** en base de datos. Solo necesarias si se activa   ***
-- *** esta opcion en el fichero de configuracion del   ***
-- *** componente central.                              ***
-- ********************************************************

-- Tabla para el guardado temporal de sesiones en BD
CREATE TABLE TB_COMP_SESIONES (
	ID VARCHAR(64) NOT NULL,
	F_MODIFICACION NUMERIC(20,0) NOT NULL,
	SESION BINARY,
	CONSTRAINT TB_SESIONES_PK PRIMARY KEY (ID)
);


-- Tabla para el guardado temporal de documentos en BD
CREATE TABLE TB_COMP_DOCUMENTOS (
	ID VARCHAR(80) NOT NULL,
	F_MODIFICACION NUMERIC(20,0) NOT NULL,
	DATOS BINARY,
	CONSTRAINT TB_DOCUMENTOS_PK PRIMARY KEY (ID)
);
