SET FILES WRITE DELAY FALSE;

CREATE TABLE VERSAO (
	MAIOR INTEGER NOT NULL,
	MENOR INTEGER NOT NULL,
	DATA TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
	CONSTRAINT VERSAO_PK PRIMARY KEY (MAIOR, MENOR)
);

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
	COMISSIONADO CHAR DEFAULT 'N' NOT NULL,
	CONSTRAINT PARTE_PK PRIMARY KEY (ID),
	CONSTRAINT PARTE_CHK_TIPO_COMISSAO CHECK (COMISSIONADO IN('S', 'N'))
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

CREATE TABLE PESSOA (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	NOME VARCHAR(200) NOT NULL,
	CODIGO VARCHAR(200),
	CONSTRAINT PESSOA_PK PRIMARY KEY (ID)
);

-- Papeis que a pessoa pode assumir
CREATE TABLE PESSOA_PARTE (
	PESSOA_ID BIGINT NOT NULL,
	PARTE_ID BIGINT NOT NULL,
	CONSTRAINT PESSOA_PARTE_PK PRIMARY KEY (PESSOA_ID, PARTE_ID),
	CONSTRAINT PESSOA_PARTE_FK_PESSOA FOREIGN KEY (PESSOA_ID) REFERENCES PESSOA(ID),
	CONSTRAINT PESSOA_PARTE_FK_PARTE FOREIGN KEY (PARTE_ID) REFERENCES PARTE(ID)
);

CREATE TABLE MEIO_PAGAMENTO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	DESCRICAO VARCHAR(200) NOT NULL,
	CONSTRAINT MEIO_PAGAMENTO_PK PRIMARY KEY (ID)
);

CREATE TABLE ENTRADA (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	DATA DATE NOT NULL,
	VALOR DECIMAL(8,2),
	DESCONTO DECIMAL(8,2),
	MEIO_PAGAMENTO_ID BIGINT NOT NULL,
	PACIENTE_ID BIGINT NOT NULL,
	CONSTRAINT ENTRADA_PK PRIMARY KEY (ID),
	CONSTRAINT ENTRADA_FK_MEIO_PAGAMENTO FOREIGN KEY (MEIO_PAGAMENTO_ID) REFERENCES MEIO_PAGAMENTO(ID),
	CONSTRAINT ENTRADA_FK_PACIENTE FOREIGN KEY (PACIENTE_ID) REFERENCES PESSOA(ID)
);

-- Produtos vinculados à entrada
CREATE TABLE ENTRADA_PRODUTO (
	ENTRADA_ID BIGINT NOT NULL,
	PRODUTO_ID BIGINT NOT NULL,
	QUANTIDADE INTEGER DEFAULT 1 NOT NULL,
	CONSTRAINT ENTRADA_PRODUTO_PK PRIMARY KEY (ENTRADA_ID, PRODUTO_ID),
	CONSTRAINT ENTRADA_PRODUTO_FK_ENTRADA FOREIGN KEY (ENTRADA_ID) REFERENCES ENTRADA(ID),
	CONSTRAINT ENTRADA_PRODUTO_FK_PRODUTO FOREIGN KEY (PRODUTO_ID) REFERENCES PRODUTO(ID)
);

-- Partes associadas à entrada com seus respectivos papeis
CREATE TABLE ENTRADA_PARTE (
	ENTRADA_ID BIGINT NOT NULL,
	PARTE_ID BIGINT NOT NULL,
	PESSOA_ID BIGINT NOT NULL,
	CONSTRAINT ENTRADA_PARTE_PK PRIMARY KEY (ENTRADA_ID, PARTE_ID, PESSOA_ID),
	CONSTRAINT ENTRADA_PARTE_FK_ENTRADA FOREIGN KEY (ENTRADA_ID) REFERENCES ENTRADA(ID),
	CONSTRAINT ENTRADA_PARTE_FK_PARTE FOREIGN KEY (PARTE_ID) REFERENCES PARTE(ID),
	CONSTRAINT ENTRADA_PARTE_FK_PESSOA FOREIGN KEY (PESSOA_ID) REFERENCES PESSOA(ID)
);

CREATE TABLE CHEQUE (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	NUMERO VARCHAR(200) NOT NULL,
	CONTA VARCHAR(200) NOT NULL,
	AGENCIA VARCHAR(200) NOT NULL,
	BANCO VARCHAR(200) NOT NULL,
	DOCUMENTO VARCHAR(200) NOT NULL,
	VALOR DECIMAL(8,2) NOT NULL,
	DATA_DEPOSITO DATE,
	OBSERVACAO VARCHAR(500),
	CLIENTE_ID BIGINT NOT NULL,
	PACIENTE_ID BIGINT NOT NULL,
	CONSTRAINT CHEQUE_PK PRIMARY KEY (ID),
	CONSTRAINT CHEQUE_FK_CLIENTE FOREIGN KEY (CLIENTE_ID) REFERENCES PESSOA(ID),
	CONSTRAINT CHEQUE_FK_PACIENTE FOREIGN KEY (PACIENTE_ID) REFERENCES PESSOA(ID)
);

INSERT INTO VERSAO(MAIOR, MENOR) VALUES(1, 0);

INSERT INTO PARTE(DESCRICAO, COMISSIONADO) VALUES('Consultório', 'S');
INSERT INTO PARTE(DESCRICAO, COMISSIONADO) VALUES('Médico', 'S');
INSERT INTO PARTE(DESCRICAO, COMISSIONADO) VALUES('Fisioterapeuta', 'S');
INSERT INTO PARTE(DESCRICAO, COMISSIONADO) VALUES('Paciente', 'N');
INSERT INTO PARTE(DESCRICAO, COMISSIONADO) VALUES('Cliente', 'N');

INSERT INTO MEIO_PAGAMENTO(DESCRICAO) VALUES('Dinheiro');
INSERT INTO MEIO_PAGAMENTO(DESCRICAO) VALUES('Cartão de Crédito (venc)');
INSERT INTO MEIO_PAGAMENTO(DESCRICAO) VALUES('Cartão de Crédito (2x)');
INSERT INTO MEIO_PAGAMENTO(DESCRICAO) VALUES('Cartão de Crédito (3x)');
INSERT INTO MEIO_PAGAMENTO(DESCRICAO) VALUES('Cartão de Débito');
INSERT INTO MEIO_PAGAMENTO(DESCRICAO) VALUES('Cheque');