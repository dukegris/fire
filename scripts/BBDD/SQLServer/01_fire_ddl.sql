-- ********************************************************
-- **************** Creacion de Tablas ********************
-- ********************************************************

-- Tabla para el guardado de los certificados
CREATE SEQUENCE TB_CERTIFICADOS_SEQ;
CREATE TABLE TB_CERTIFICADOS (
	ID_CERTIFICADO BIGINT NOT NULL DEFAULT (NEXT VALUE FOR TB_CERTIFICADOS_SEQ),
	NOMBRE_CERT VARCHAR(45) NOT NULL,
	FEC_ALTA DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
	CERT_PRINCIPAL TEXT,
	CERT_BACKUP TEXT,
	HUELLA_PRINCIPAL VARCHAR(45),
	HUELLA_BACKUP VARCHAR(45),
	CONSTRAINT TB_CERTIFICADOS_PK PRIMARY KEY (ID_CERTIFICADO)
);
GO



-- Tabla para el guardado de las aplicaciones
CREATE TABLE TB_APLICACIONES (
	ID VARCHAR(48) NOT NULL,
	NOMBRE VARCHAR(45) NOT NULL,
	FECHA_ALTA DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FK_CERTIFICADO BIGINT NOT NULL,
	HABILITADO NUMERIC(1,0) DEFAULT 1,
	CONSTRAINT TB_APLICACIONES_PK PRIMARY KEY (ID)
);
ALTER TABLE TB_APLICACIONES ADD CONSTRAINT TB_APLICACIONES_FK FOREIGN KEY (FK_CERTIFICADO) REFERENCES TB_CERTIFICADOS(ID_CERTIFICADO);
GO



-- Tabla para el guardado de los usuarios
CREATE SEQUENCE TB_USUARIOS_SEQ;
CREATE TABLE TB_USUARIOS (
	ID_USUARIO BIGINT NOT NULL DEFAULT (NEXT VALUE FOR TB_USUARIOS_SEQ),
	NOMBRE_USUARIO VARCHAR(30) NOT NULL,
	CLAVE VARCHAR(2000) NOT NULL,
	NOMBRE VARCHAR(45) NOT NULL,
	APELLIDOS VARCHAR(120) NOT NULL,
	CORREO_ELEC VARCHAR(45),
	TELF_CONTACTO VARCHAR(45),
	FK_ROL BIGINT NOT NULL,
	FEC_ALTA DATETIME2(6) DEFAULT SYSDATETIME(),
	USU_DEFECTO NUMERIC(1,0) DEFAULT 0,
	CODIGO_RENOVACION VARCHAR(100) DEFAULT NULL,
	FEC_RENOVACION DATETIME DEFAULT NULL,
	REST_CLAVE NUMERIC(1,0) DEFAULT 0,	
	CONSTRAINT TB_USUARIOS_PK PRIMARY KEY (ID_USUARIO),
	CONSTRAINT TB_USUARIOS_UK1 UNIQUE (NOMBRE_USUARIO),
	CONSTRAINT TB_USUARIOS_UK2 UNIQUE (CODIGO_RENOVACION)
);
GO



-- Tabla para el guardado de los roles
CREATE TABLE TB_ROLES (
	ID BIGINT NOT NULL,
	NOMBRE_ROL VARCHAR(45) NOT NULL,
	PERMISOS VARCHAR(45),
	CONSTRAINT TB_ROLES_PK PRIMARY KEY (ID),
	CONSTRAINT TB_ROLES_UK UNIQUE (NOMBRE_ROL)
);
ALTER TABLE TB_USUARIOS ADD CONSTRAINT TB_USUARIOS_FK FOREIGN KEY (FK_ROL) REFERENCES TB_ROLES(ID);
GO


-- Tabla de relacion de responsables y aplicaciones
CREATE TABLE TB_RESPONSABLE_DE_APLICACIONES (
	ID_RESPONSABLES BIGINT NOT NULL,
	ID_APLICACIONES VARCHAR(48) NOT NULL,
	CONSTRAINT TB_RESPONSABLES_PK PRIMARY KEY (ID_RESPONSABLES,ID_APLICACIONES)
);
GO



-- Tabla para el guardado de las referencias a los servidores de log
CREATE SEQUENCE TB_SERVIDORES_LOG_SEQ;
CREATE TABLE TB_SERVIDORES_LOG (
	ID_SERVIDOR BIGINT NOT NULL DEFAULT (NEXT VALUE FOR TB_SERVIDORES_LOG_SEQ),
	NOMBRE VARCHAR(45) NOT NULL,
	URL_SERVICIO_LOG VARCHAR(500) NOT NULL,
	CLAVE VARCHAR(45) NOT NULL,
	VERIFICAR_SSL NUMERIC(1,0) NOT NULL,
	CONSTRAINT TB_SERVIDORES_LOG_PK PRIMARY KEY (ID_SERVIDOR),
	CONSTRAINT TB_SERVIDORES_LOG_UK1 UNIQUE (NOMBRE),
	CONSTRAINT TB_SERVIDORES_LOG_UK2 UNIQUE (URL_SERVICIO_LOG)
);
GO


-- Tabla de estadisticas de las firmas
CREATE SEQUENCE TB_FIRMAS_SEQ;
CREATE TABLE TB_FIRMAS (
	ID BIGINT NOT NULL DEFAULT (NEXT VALUE FOR TB_FIRMAS_SEQ),
	FECHA DATETIME2(6) NOT NULL,
	APLICACION VARCHAR(45) NOT NULL,
	FORMATO VARCHAR(20) NOT NULL,
	FORMATO_MEJORADO VARCHAR(20),
	ALGORITMO VARCHAR(20) NOT NULL,
	PROVEEDOR VARCHAR(45) NOT NULL,
	NAVEGADOR VARCHAR(20) NOT NULL,
	CORRECTA NUMERIC(1,0) NOT NULL,
	TOTAL NUMERIC DEFAULT 0 NOT NULL,
	CONSTRAINT TB_FIRMAS_PK PRIMARY KEY (ID)
);
GO



-- Tabla de estadisticas de las transacciones
CREATE SEQUENCE TB_TRANSACCIONES_SEQ;
CREATE TABLE TB_TRANSACCIONES (
	ID BIGINT NOT NULL DEFAULT (NEXT VALUE FOR TB_TRANSACCIONES_SEQ),
	FECHA DATETIME2(6) NOT NULL,
	APLICACION VARCHAR(45) NOT NULL,
	OPERACION VARCHAR(10) NOT NULL,
	PROVEEDOR VARCHAR(45) NOT NULL,
	PROVEEDOR_FORZADO NUMERIC(1,0) NOT NULL,
	CORRECTA NUMERIC(1,0) NOT NULL,
	TAMANNO NUMERIC DEFAULT 0 NOT NULL,
	TOTAL NUMERIC DEFAULT 0 NOT NULL,
	CONSTRAINT TB_TRANSACCIONES_PK PRIMARY KEY (ID)
);
GO



-- Tabla de auditoria de las transacciones
CREATE SEQUENCE TB_AUDIT_TRANSACCIONES_SEQ;
CREATE TABLE TB_AUDIT_TRANSACCIONES (
	ID BIGINT NOT NULL DEFAULT (NEXT VALUE FOR TB_AUDIT_TRANSACCIONES_SEQ),
	FECHA DATETIME2(6) NOT NULL,
	ID_APLICACION VARCHAR(48) NOT NULL,
	NOMBRE_APLICACION VARCHAR(45) NOT NULL,
	ID_TRANSACCION VARCHAR(45) NOT NULL,
	OPERACION VARCHAR(20) NOT NULL,
	OPERACION_CRIPTOGRAFICA VARCHAR(20) NOT NULL,
	FORMATO VARCHAR(20) NOT NULL,
	FORMATO_ACTUALIZADO VARCHAR(20),
	ALGORITMO VARCHAR(20) NOT NULL,
	PROVEEDOR VARCHAR(45) NOT NULL,
	PROVEEDOR_FORZADO NUMERIC(1,0) NOT NULL,
	NAVEGADOR VARCHAR(20) NOT NULL,
	TAMANNO NUMERIC DEFAULT 0,
	NODO VARCHAR(45),
	RESULTADO NUMERIC(1,0) NOT NULL,
	ERROR_DETALLE VARCHAR(150),
	CONSTRAINT TB_PETICIONES_PK PRIMARY KEY (ID)
);
GO



-- Tabla de auditoria de las firmas
CREATE SEQUENCE TB_AUDIT_FIRMAS_SEQ;
CREATE TABLE TB_AUDIT_FIRMAS (
	ID BIGINT NOT NULL DEFAULT (NEXT VALUE FOR TB_AUDIT_FIRMAS_SEQ),
	ID_TRANSACCION VARCHAR(45) NOT NULL,
	ID_INT_LOTE VARCHAR(45),
	OPERACION_CRIPTOGRAFICA VARCHAR(20),
	FORMATO VARCHAR(20) NOT NULL,
	FORMATO_ACTUALIZADO VARCHAR(20),
	TAMANNO NUMERIC DEFAULT 0,
	RESULTADO NUMERIC(1,0) NOT NULL,
	ERROR_DETALLE VARCHAR(150),
	CONSTRAINT TB_PETICIONES_FIRMAS_LOTE_PK PRIMARY KEY (ID)
);
GO
