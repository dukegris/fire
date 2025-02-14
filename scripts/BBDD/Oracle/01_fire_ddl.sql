-- ********************************************************
-- **************** Creacion de Tablas ********************
-- ********************************************************

CREATE TABLE "TB_CERTIFICADOS" (
    "ID_CERTIFICADO"   NUMBER NOT NULL,
    "NOMBRE_CERT"      VARCHAR2(45) NOT NULL,
    "FEC_ALTA"         TIMESTAMP NOT NULL,
    "CERT_PRINCIPAL"   CLOB,
    "CERT_BACKUP"      CLOB,
    "HUELLA_PRINCIPAL" VARCHAR2(45),
    "HUELLA_BACKUP"    VARCHAR2(45),
    constraint  "TB_CERTIFICADOS_PK" primary key ("ID_CERTIFICADO")
);

ALTER TABLE  "TB_CERTIFICADOS" modify
("FEC_ALTA" TIMESTAMP default SYSDATE);

CREATE SEQUENCE "TB_CERTIFICADOS_SEQ"; 

CREATE OR REPLACE TRIGGER "BI_TB_CERTIFICADOS"  
  before insert on "TB_CERTIFICADOS"              
  for each row 
begin  
  if :NEW."ID_CERTIFICADO" is null then
    select "TB_CERTIFICADOS_SEQ".nextval into :NEW."ID_CERTIFICADO" from dual;
  end if;
end;
/

ALTER TRIGGER "BI_TB_CERTIFICADOS" ENABLE;


CREATE TABLE "TB_APLICACIONES" (
  "ID" VARCHAR2(48) NOT NULL,
  "NOMBRE" VARCHAR2(45) NOT NULL,
  "FECHA_ALTA" TIMESTAMP NOT NULL,
  "FK_CERTIFICADO" NUMBER NOT NULL,
  "HABILITADO" NUMBER(1,0) DEFAULT 1, 
  constraint  "TB_APLICACIONES_PK" primary key ("ID")
); 

ALTER TABLE  "TB_APLICACIONES" modify
("FECHA_ALTA" TIMESTAMP default SYSDATE);

ALTER TABLE "TB_APLICACIONES" add constraint
"TB_APLICACIONES_FK" foreign key ("FK_CERTIFICADO") references "TB_CERTIFICADOS" ("ID_CERTIFICADO");


CREATE TABLE  "TB_USUARIOS" 
   ("ID_USUARIO" NUMBER NOT NULL, 
	"NOMBRE_USUARIO" VARCHAR2(30) NOT NULL, 
	"CLAVE" VARCHAR2(2000) NOT NULL, 
	"NOMBRE" VARCHAR2(45) NOT NULL, 
	"APELLIDOS" VARCHAR2(120) NOT NULL, 
	"CORREO_ELEC" VARCHAR2(45), 
	"TELF_CONTACTO" VARCHAR2(45), 
	"FK_ROL" NUMBER NOT NULL,
	"FEC_ALTA" TIMESTAMP (6) DEFAULT SYSDATE, 
	"USU_DEFECTO" NUMBER(1,0) DEFAULT 0, 
	"CODIGO_RENOVACION" VARCHAR2(100) DEFAULT NULL,
	"FEC_RENOVACION" TIMESTAMP DEFAULT NULL,
	"REST_CLAVE" NUMBER(1,0) DEFAULT 0,	
	 CONSTRAINT "TB_USUARIOS_PK" PRIMARY KEY ("ID_USUARIO"), 
	 CONSTRAINT "TB_USUARIOS_UK1" UNIQUE ("NOMBRE_USUARIO"),
	 CONSTRAINT "TB_USUARIOS_UK2" UNIQUE ("CODIGO_RENOVACION")
);

CREATE SEQUENCE "TB_USUARIOS_SEQ"; 

CREATE OR REPLACE TRIGGER  "BI_TB_USUARIOS" 
  before insert on "TB_USUARIOS"               
  for each row  
begin   
  if :NEW."ID_USUARIO" is null then 
    select "TB_USUARIOS_SEQ".nextval into :NEW."ID_USUARIO" from dual; 
  end if; 
end;
/

ALTER TRIGGER "BI_TB_USUARIOS" ENABLE;


-- Tabla para el guardado de los roles

CREATE TABLE "TB_ROLES" (
  "ID" NUMBER NOT NULL,
  "NOMBRE_ROL" VARCHAR2(45) NOT NULL,
  "PERMISOS"   VARCHAR2(45),
  CONSTRAINT "TB_ROLES_PK" PRIMARY KEY ("ID"),
  CONSTRAINT "TB_ROLES_UK" UNIQUE ("NOMBRE_ROL")
);

ALTER TABLE "TB_USUARIOS" ADD CONSTRAINT
"TB_USUARIOS_FK" FOREIGN KEY ("FK_ROL") REFERENCES "TB_ROLES" ("ID");

-- Tabla de relacion de responsables y aplicaciones

CREATE TABLE "TB_RESPONSABLE_DE_APLICACIONES" (
  "ID_RESPONSABLES" NUMBER NOT NULL,
  "ID_APLICACIONES" VARCHAR2(48) NOT NULL,
  CONSTRAINT "TB_RESPONSABLES_PK" PRIMARY KEY ("ID_RESPONSABLES","ID_APLICACIONES")
);

-- Tabla para el guardado de las referencias a los servidores de log

CREATE TABLE "TB_SERVIDORES_LOG" (
  "ID_SERVIDOR" NUMBER NOT NULL,
  "NOMBRE" VARCHAR2(45) NOT NULL, 
  "URL_SERVICIO_LOG" VARCHAR2(500) NOT NULL, 
  "CLAVE" VARCHAR2(45) NOT NULL, 
  "VERIFICAR_SSL" NUMBER(1,0) NOT NULL,
  CONSTRAINT "TB_SERVIDORES_LOG_PK" PRIMARY KEY ("ID_SERVIDOR"), 
  CONSTRAINT "TB_SERVIDORES_LOG_UK1" UNIQUE ("NOMBRE"),
  CONSTRAINT "TB_SERVIDORES_LOG_UK2" UNIQUE ("URL_SERVICIO_LOG")
);

CREATE SEQUENCE "TB_SERVIDORES_LOG_SEQ"; 

CREATE OR REPLACE TRIGGER "BI_TB_SERVIDORES_LOG"  
  before insert on "TB_SERVIDORES_LOG"              
  for each row 
begin  
  if :NEW."ID_SERVIDOR" is null then
    select "TB_SERVIDORES_LOG_SEQ".nextval into :NEW."ID_SERVIDOR" from dual;
  end if;
end;
/

ALTER TRIGGER "BI_TB_SERVIDORES_LOG" ENABLE;

-- Tabla de estadisticas de las firmas

CREATE TABLE "TB_FIRMAS" (
  "ID" NUMBER NOT NULL,
  "FECHA" TIMESTAMP (6) NOT NULL,
  "APLICACION" VARCHAR2(45) NOT NULL, 
  "FORMATO" VARCHAR2(20) NOT NULL, 
  "FORMATO_MEJORADO" VARCHAR2(20),
  "ALGORITMO" VARCHAR2(20) NOT NULL, 
  "PROVEEDOR" VARCHAR2(45) NOT NULL, 
  "NAVEGADOR" VARCHAR2(20) NOT NULL, 
  "CORRECTA" NUMBER(1,0) NOT NULL,
  "TOTAL" NUMBER DEFAULT 0 NOT NULL,
  CONSTRAINT "TB_FIRMAS_PK" PRIMARY KEY ("ID")
);

CREATE SEQUENCE "TB_FIRMAS_SEQ"; 

CREATE OR REPLACE TRIGGER "BI_TB_FIRMAS"  
  before insert on "TB_FIRMAS"              
  for each row 
begin  
  if :NEW."ID" is null then
    select "TB_FIRMAS_SEQ".nextval into :NEW."ID" from dual;
  end if;
end;
/

ALTER TRIGGER "BI_TB_FIRMAS" ENABLE;

-- Tabla de estadisticas de las transacciones

CREATE TABLE "TB_TRANSACCIONES" (
  "ID" NUMBER NOT NULL,
  "FECHA" TIMESTAMP (6) NOT NULL,
  "APLICACION" VARCHAR2(45) NOT NULL,
  "OPERACION" VARCHAR2(10) NOT NULL,
  "PROVEEDOR" VARCHAR2(45) NOT NULL,
  "PROVEEDOR_FORZADO" NUMBER(1,0) NOT NULL,
  "CORRECTA" NUMBER(1,0) NOT NULL,
  "TAMANNO" NUMBER DEFAULT 0 NOT NULL,
  "TOTAL" NUMBER DEFAULT 0 NOT NULL,
  CONSTRAINT "TB_TRANSACCIONES_PK" PRIMARY KEY ("ID")
);

CREATE SEQUENCE "TB_TRANSACCIONES_SEQ"; 

CREATE OR REPLACE TRIGGER "BI_TB_TRANSACCIONES"  
  before insert on "TB_TRANSACCIONES"              
  for each row 
begin  
  if :NEW."ID" is null then
    select "TB_TRANSACCIONES_SEQ".nextval into :NEW."ID" from dual;
  end if;
end;
/

ALTER TRIGGER "BI_TB_TRANSACCIONES" ENABLE;

-- Tabla de auditoria de las transacciones

CREATE TABLE "TB_AUDIT_TRANSACCIONES" (
	"ID" NUMBER NOT NULL,
	"FECHA" TIMESTAMP (6) NOT NULL,
	"ID_APLICACION" VARCHAR2(48) NOT NULL,
	"NOMBRE_APLICACION" VARCHAR2(45) NOT NULL,
	"ID_TRANSACCION" VARCHAR2(45) NOT NULL,
  	"OPERACION" VARCHAR2(20) NOT NULL,
  	"OPERACION_CRIPTOGRAFICA" VARCHAR2(20) NOT NULL,
  	"FORMATO" VARCHAR2(20) NOT NULL, 
  	"FORMATO_ACTUALIZADO" VARCHAR2(20),
  	"ALGORITMO" VARCHAR2(20) NOT NULL, 
  	"PROVEEDOR" VARCHAR2(45) NOT NULL,
  	"PROVEEDOR_FORZADO" NUMBER(1,0) NOT NULL,
  	"NAVEGADOR" VARCHAR2(20) NOT NULL, 
  	"TAMANNO" NUMBER DEFAULT 0,
  	"NODO" VARCHAR2(45),
  	"RESULTADO" NUMBER(1,0) NOT NULL,
  	"ERROR_DETALLE" VARCHAR2(150),
  	CONSTRAINT "TB_PETICIONES_PK" PRIMARY KEY ("ID")
);

CREATE SEQUENCE "TB_AUDIT_TRANSACCIONES_SEQ"; 

CREATE OR REPLACE TRIGGER "BI_TB_AUDIT_TRANSACCIONES"  
  before insert on "TB_AUDIT_TRANSACCIONES"              
  for each row 
begin  
  if :NEW."ID" is null then
    select "TB_AUDIT_TRANSACCIONES_SEQ".nextval into :NEW."ID" from dual;
  end if;
end;
/

ALTER TRIGGER "BI_TB_AUDIT_TRANSACCIONES" ENABLE;

-- Tabla de auditoria de las firmas

CREATE TABLE "TB_AUDIT_FIRMAS" (
	"ID" NUMBER NOT NULL,
	"ID_TRANSACCION" VARCHAR2(45) NOT NULL,
	"ID_INT_LOTE" VARCHAR2(45), 
  	"OPERACION_CRIPTOGRAFICA" VARCHAR2(20),
  	"FORMATO" VARCHAR2(20) NOT NULL, 
  	"FORMATO_ACTUALIZADO" VARCHAR2(20),
  	"TAMANNO" NUMBER DEFAULT 0,
  	"RESULTADO" NUMBER(1,0) NOT NULL,
  	"ERROR_DETALLE" VARCHAR2(150),
  	CONSTRAINT "TB_PETICIONES_FIRMAS_LOTE_PK" PRIMARY KEY ("ID")
);

CREATE SEQUENCE "TB_AUDIT_FIRMAS_SEQ"; 

CREATE OR REPLACE TRIGGER "BI_TB_AUDIT_FIRMAS"  
  before insert on "TB_AUDIT_FIRMAS"              
  for each row 
begin  
  if :NEW."ID" is null then
    select "TB_AUDIT_FIRMAS_SEQ".nextval into :NEW."ID" from dual;
  end if;
end;
/

ALTER TRIGGER "BI_TB_AUDIT_FIRMAS" ENABLE;