SET FILES WRITE DELAY FALSE;

CREATE TABLE CATEGORIA (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	DESCRICAO VARCHAR(200),
	CONSTRAINT CATEGORIA_PK PRIMARY KEY (ID)
);

CREATE TABLE PRODUTO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	CODIGO VARCHAR(30),
	DESCRICAO VARCHAR(200),
	CUSTO DECIMAL(8,2) NOT NULL,
	PRECO DECIMAL(8,2) NOT NULL,
	CATEGORIA_ID BIGINT NOT NULL,
	CONSTRAINT PRODUTO_PK PRIMARY KEY (ID),
	CONSTRAINT PRODUTO_FK_CATEGORIA FOREIGN KEY (CATEGORIA_ID) REFERENCES CATEGORIA(ID)
);

CREATE TABLE PARTE (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	DESCRICAO VARCHAR(200) NOT NULL,
	CONSTRAINT PARTE_PK PRIMARY KEY (ID)
);

CREATE TABLE COMISSAO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	PORCENTAGEM DECIMAL(5,2),
	VALOR DECIMAL(8,2),
	PARTE_ID BIGINT NOT NULL,
	CATEGORIA_ID BIGINT NOT NULL,
	CONSTRAINT COMISSAO_PK PRIMARY KEY (ID),
	CONSTRAINT COMISSAO_FK_PARTE FOREIGN KEY (PARTE_ID) REFERENCES PARTE(ID),
	CONSTRAINT COMISSAO_FK_CATEGORIA FOREIGN KEY (CATEGORIA_ID) REFERENCES CATEGORIA(ID),
	CONSTRAINT COMISSAO_CK1 CHECK(PORCENTAGEM IS NOT NULL OR VALOR IS NOT NULL)
);

INSERT INTO PARTE(DESCRICAO) VALUES('Consultório');
INSERT INTO PARTE(DESCRICAO) VALUES('Médico');
INSERT INTO PARTE(DESCRICAO) VALUES('Fisioterapeuta');