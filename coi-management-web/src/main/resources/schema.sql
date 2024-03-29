SET FILES WRITE DELAY FALSE;

CREATE TABLE CATEGORIA (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	DESCRICAO VARCHAR(200),
	TIPO_COMISSAO VARCHAR(10) NOT NULL,
	CONSTRAINT CATEGORIA_PK PRIMARY KEY (ID),
	CONSTRAINT CATEGORIA_DESCRICAO_UK UNIQUE (DESCRICAO),
	CONSTRAINT CATEGORIA_CHK_TIPO_COMISSAO CHECK (TIPO_COMISSAO IN ('PERCENTUAL', 'VALOR'))
);

CREATE TABLE PRODUTO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	CODIGO VARCHAR(30),
	DESCRICAO VARCHAR(200),
	PRECO DECIMAL(8,2) NOT NULL,
	CATEGORIA_ID BIGINT NOT NULL,
	ESTOCAVEL CHAR(1) NOT NULL,
	CONSTRAINT PRODUTO_PK PRIMARY KEY (ID),
	CONSTRAINT PRODUTO_FK_CATEGORIA FOREIGN KEY (CATEGORIA_ID) REFERENCES CATEGORIA(ID),
	CONSTRAINT PRODUTO_CODIGO_UK UNIQUE (CODIGO),
	CONSTRAINT PRODUTO_DESCRICAO_UK UNIQUE (DESCRICAO),
	CONSTRAINT PRODUTO_CHK_ESTOCAVEL CHECK (ESTOCAVEL IN('S', 'N'))
);

CREATE TABLE PRODUTO_CUSTO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	PRODUTO_ID BIGINT NOT NULL,
	DATA_INICIO_VIGENCIA DATE NOT NULL,
	DATA_FIM_VIGENCIA DATE,
	CUSTO DECIMAL(8,2) NOT NULL,
	CONSTRAINT PRODUTO_CUSTO_PK PRIMARY KEY (ID),
	CONSTRAINT PRODUTO_CUSTO_FK_PRODUTO FOREIGN KEY (PRODUTO_ID) REFERENCES PRODUTO(ID)
);

CREATE TABLE COMISSAO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	PORCENTAGEM DECIMAL(5,2),
	VALOR DECIMAL(8,2),
	RESTANTE CHAR(1),
	CATEGORIA_ID BIGINT NOT NULL,
	PARTE VARCHAR(17) NOT NULL,
	CONSTRAINT COMISSAO_PK PRIMARY KEY (ID),
	CONSTRAINT COMISSAO_FK_CATEGORIA FOREIGN KEY (CATEGORIA_ID) REFERENCES CATEGORIA(ID),
	CONSTRAINT COMISSAO_PARTE_UK UNIQUE (CATEGORIA_ID, PARTE),
	CONSTRAINT COMISSAO_CHK_RESTANTE CHECK (RESTANTE IN ('S', 'N'))
);

CREATE TABLE PESSOA (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	NOME VARCHAR(200) NOT NULL,
	PREFIXO CHAR(1) NOT NULL,
	CODIGO INTEGER NOT NULL,
	SEXO VARCHAR(9),
	DATA_NASCIMENTO DATE,
	CONSTRAINT PESSOA_PK PRIMARY KEY (ID),
	CONSTRAINT PESSOA_PREFIXO_CODIGO_UK UNIQUE (PREFIXO, CODIGO),
	CONSTRAINT PESSOA_CHK_SEXO CHECK (SEXO IN ('FEMININO', 'MASCULINO', NULL))
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
	CONSTRAINT CHEQUE_FK_PACIENTE FOREIGN KEY (PACIENTE_ID) REFERENCES PESSOA(ID),
	CONSTRAINT CHEQUE_UK UNIQUE (NUMERO, CONTA, AGENCIA, BANCO)
);

CREATE TABLE ENTRADA (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	DATA TIMESTAMP NOT NULL,
	MEIO_PAGAMENTO VARCHAR(100) NOT NULL,
	PACIENTE_ID BIGINT NOT NULL,
	CONSTRAINT ENTRADA_PK PRIMARY KEY (ID),
	CONSTRAINT ENTRADA_CHK_MEIO_PAGAMENTO CHECK (MEIO_PAGAMENTO IN ('DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_CREDITO_2X', 'CARTAO_CREDITO_3X', 'CARTAO_DEBITO', 'CHEQUE')),
	CONSTRAINT ENTRADA_FK_PACIENTE FOREIGN KEY (PACIENTE_ID) REFERENCES PESSOA(ID)
);

-- Produtos vinculados à entrada
CREATE TABLE ENTRADA_PRODUTO (
	ENTRADA_ID BIGINT NOT NULL,
	PRODUTO_ID BIGINT NOT NULL,
	QUANTIDADE INTEGER DEFAULT 1 NOT NULL,
	VALOR DECIMAL(8,2) NOT NULL,
	DESCONTO DECIMAL(8,2) DEFAULT 0,
	CONSTRAINT ENTRADA_PRODUTO_PK PRIMARY KEY (ENTRADA_ID, PRODUTO_ID),
	CONSTRAINT ENTRADA_PRODUTO_FK_ENTRADA FOREIGN KEY (ENTRADA_ID) REFERENCES ENTRADA(ID),
	CONSTRAINT ENTRADA_PRODUTO_FK_PRODUTO FOREIGN KEY (PRODUTO_ID) REFERENCES PRODUTO(ID)
);

-- Partes associadas à entrada com seus respectivos papeis
CREATE TABLE ENTRADA_PARTE (
	ENTRADA_ID BIGINT NOT NULL,
	PESSOA_ID BIGINT NOT NULL,
	PARTE VARCHAR(17) NOT NULL,
	CONSTRAINT ENTRADA_PARTE_FK_ENTRADA FOREIGN KEY (ENTRADA_ID) REFERENCES ENTRADA(ID),
	CONSTRAINT ENTRADA_PARTE_FK_PESSOA FOREIGN KEY (PESSOA_ID) REFERENCES PESSOA(ID),
	CONSTRAINT ENTRADA_PARTE_PK PRIMARY KEY (ENTRADA_ID, PARTE, PESSOA_ID)
);

CREATE TABLE ENTRADA_CHEQUE (
	ENTRADA_ID BIGINT NOT NULL,
	CHEQUE_ID BIGINT NOT NULL,
	CONSTRAINT ENTRADA_CHEQUE_PK PRIMARY KEY (ENTRADA_ID, CHEQUE_ID),
	CONSTRAINT ENTRADA_CHEQUE_FK_ENTRADA FOREIGN KEY (ENTRADA_ID) REFERENCES ENTRADA(ID),
	CONSTRAINT ENTRADA_CHEQUE_FK_CHEQUE FOREIGN KEY (CHEQUE_ID) REFERENCES CHEQUE(ID)
);

CREATE TABLE LAUDO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	PACIENTE_ID BIGINT NOT NULL,
	MEDICO_ID BIGINT NOT NULL,
	DATA DATE NOT NULL,
	STATUS_HORMONAL VARCHAR(20) NOT NULL,
	COLUNA_LOMBAR_L1 CHAR(1) DEFAULT 'S' NOT NULL,
	COLUNA_LOMBAR_L2 CHAR(1) DEFAULT 'S' NOT NULL,
	COLUNA_LOMBAR_L3 CHAR(1) DEFAULT 'S' NOT NULL,
	COLUNA_LOMBAR_L4 CHAR(1) DEFAULT 'S' NOT NULL,
	COLUNA_LOMBAR_DENSIDADE DECIMAL(4,3),
	COLUNA_LOMBAR_TSCORE DECIMAL(3,1),
	COLUNA_LOMBAR_ZSCORE DECIMAL(3,1),
	COLO_FEMUR_DENSIDADE DECIMAL(4,3),
	COLO_FEMUR_TSCORE DECIMAL(3,1),
	COLO_FEMUR_ZSCORE DECIMAL(3,1),
	FEMUR_TOTAL_DENSIDADE DECIMAL(4,3),
	FEMUR_TOTAL_TSCORE DECIMAL(3,1),
	FEMUR_TOTAL_ZSCORE DECIMAL(3,1),
	RADIO_TERCO_DENSIDADE DECIMAL(4,3),
	RADIO_TERCO_TSCORE DECIMAL(3,1),
	RADIO_TERCO_ZSCORE DECIMAL(3,1),
	CORPO_INTEIRO_DENSIDADE DECIMAL(4,3),
	CORPO_INTEIRO_ZSCORE DECIMAL(3,1),
	CONCLUSAO VARCHAR(21) NOT NULL,
	CONSTRAINT LAUDO_PK PRIMARY KEY (ID),
	CONSTRAINT LAUDO_FK_PACIENTE FOREIGN KEY (PACIENTE_ID) REFERENCES PESSOA(ID),
	CONSTRAINT LAUDO_FK_MEDICO FOREIGN KEY (MEDICO_ID) REFERENCES PESSOA(ID),
	CONSTRAINT LAUDO_CHK_STATUS_HORMONAL CHECK (STATUS_HORMONAL IN ('PRE_MENOPAUSAL', 'POS_MENOPAUSAL', 'TRANSICAO_MENOPAUSAL'))
);

CREATE TABLE LAUDO_OBSERVACAO_OPCAO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	CODIGO INTEGER NOT NULL,
	DESCRICAO VARCHAR(500) NOT NULL,
	ROTULO VARCHAR(100) NOT NULL,
	CONSTRAINT LAUDO_OBSERVACAO_OPCAO_PK PRIMARY KEY (ID)
);

CREATE TABLE LAUDO_OBSERVACAO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	LAUDO_ID BIGINT NOT NULL,
	LAUDO_OBSERVACAO_OPCAO_ID BIGINT NOT NULL,
	CONSTRAINT LAUDO_OBSERVACAO_PK PRIMARY KEY (ID),
	CONSTRAINT LAUDO_OBSERVACAO_FK_LAUDO FOREIGN KEY (LAUDO_ID) REFERENCES LAUDO(ID),
	CONSTRAINT LAUDO_OBSERVACAO_FK_LAUDO_OBSERVACAO_OPCAO FOREIGN KEY (LAUDO_OBSERVACAO_OPCAO_ID) REFERENCES LAUDO_OBSERVACAO_OPCAO(ID)
);

CREATE TABLE LAUDO_OBSERVACAO_VALUE (
	LAUDO_OBSERVACAO_ID BIGINT NOT NULL,
	NOME VARCHAR(20) NOT NULL,
	VALOR VARCHAR(100) NOT NULL,
	CONSTRAINT LAUDO_OBSERVACAO_VALUE_UK UNIQUE (LAUDO_OBSERVACAO_ID, NOME)
);

INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(1, 'Presença de alterações estruturais na coluna lombar que podem superestimar a real densidade óssea vertebral', 'Presença de alterações estruturais...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(2, 'À critério clínico, sugerimos estudo radiográfico da coluna lombar para avaliar fraturas ou alterações degenerativas, devido à diferença de densidade mineral óssea observada entre {segmentoSText}. Na presença de fraturas vertebrais, considerar o diagnóstico de osteoporose, independente dos valores da densitometria', 'À critério clínico, sugerimos estudo radiográfico... Na presença...'); 
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(3, 'À critério clínico, sugerimos estudo radiográfico da coluna lombar para avaliar fraturas ou alterações degenerativas, devido à diferença de densidade mineral óssea observada entre {segmentoSText}', 'À critério clínico, sugerimos estudo radiográfico...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(4, 'Devido à presença de alterações estruturais na coluna lombar procedemos a análise no antebraço', 'Devido à presença de alterações estruturais...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(5, 'Presença de artefato ao nível de {segmentoSText}', 'Presença de artefato ao nível de...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(6, 'Presença de provável vértebra de transição (VT). Sugerimos estudo radiográfico para melhor análise', 'Presença de provável vértebra de transição...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(7, 'Devido {impedimentoSText}, não foi possível análise válida do fêmur direito. Por esse motivo, procedemos a análise de fêmur esquerdo', 'Devido dificuldade de rotação do quadril... fêmur direito...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(8, 'Devido {impedimentoSText}, não foi possível análise válida de fêmur, tanto a direita quanto a esquerda', 'Devido dificuldade de rotação do quadril... ambos...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(9, 'Sugerimos densitometria óssea de antebraço (Radio 33%)', 'Sugerimos densitometria óssea...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(10, 'Presença de alterações estruturais no quadril que podem superestimar a real densidade óssea do fêmur', 'Presença de alterações estruturais...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(11, 'Sugerimos afastar causa secundária de baixa densidade óssea para a idade', 'Sugerimos afastar causa secundária... baixa densidade...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(12, 'Sugerimos afastar causa secundária de osteoporose (Z-score <= -2)', 'Sugerimos afastar causa secundária... osteoporose...');
INSERT INTO LAUDO_OBSERVACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(13, '{observacaoLText}', 'Observação');

CREATE TABLE LAUDO_COMPARACAO_OPCAO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	CODIGO INTEGER NOT NULL,
	DESCRICAO VARCHAR(500) NOT NULL,
	ROTULO VARCHAR(100) NOT NULL,
	CONSTRAINT LAUDO_COMPARACAO_OPCAO_PK PRIMARY KEY (ID)
);

CREATE TABLE LAUDO_COMPARACAO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	LAUDO_ID BIGINT NOT NULL,
	LAUDO_COMPARACAO_OPCAO_ID BIGINT NOT NULL,
	CONSTRAINT LAUDO_COMPARACAO_PK PRIMARY KEY (ID),
	CONSTRAINT LAUDO_COMPARACAO_FK_LAUDO FOREIGN KEY (LAUDO_ID) REFERENCES LAUDO(ID),
	CONSTRAINT LAUDO_COMPARACAO_FK_LAUDO_COMPARACAO_OPCAO FOREIGN KEY (LAUDO_COMPARACAO_OPCAO_ID) REFERENCES LAUDO_COMPARACAO_OPCAO(ID)
);

CREATE TABLE LAUDO_COMPARACAO_VALUE (
	LAUDO_COMPARACAO_ID BIGINT NOT NULL,
	NOME VARCHAR(20) NOT NULL,
	VALOR VARCHAR(100) NOT NULL,
	CONSTRAINT LAUDO_COMPARACAO_VALUE_UK UNIQUE (LAUDO_COMPARACAO_ID, NOME)
);

INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(1, 'Não há exames anteriores neste serviço para comparação', 'Não há exames anteriores...');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(2, 'Considerando-se a mínima variação significativa (MVS) deste serviço de {densidadeColunaSText}g/cm² para coluna e de {densidadeFemurSText}g/cm²  para fêmur total, em comparação com a DMO de {dataDate} houve:', 'Considerando-se a mínima variação significativa...');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(3, 'Manutenção da massa óssea na coluna lombar', 'Manutenção da massa óssea na coluna lombar');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(4, 'Manutenção da massa óssea no fêmur total', 'Manutenção da massa óssea no fêmur total');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(5, 'Perda de densidade mineral óssea de {densidadePercent} ± {mvcSText}g/cm² na coluna lombar', 'Perda de densidade óssea na coluna');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(6, 'Perda de densidade mineral óssea de {densidadePercent} ± {mvcSText}g/cm² no fêmur total', 'Perda de densidade óssea no fêmur');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(7, 'Ganho de densidade mineral óssea de {densidadePercent} ± {mvcSText}g/cm² na coluna lombar', 'Ganho de densidade óssea na coluna');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(8, 'Ganho de densidade mineral óssea de {densidadePercent} ± {mvcSText}g/cm² no fêmur total', 'Ganho de densidade óssea no fêmur');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(9, 'Sugerimos não utilizar a coluna lombar para diagnóstico, classificação de risco de fraturas ou monitorização terapêutica, devido à presença de alterações vertebrais', 'Sugerimos não utilizar a coluna lombar...');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(10, 'Sugerimos não utilizar o fêmur proximal para diagnóstico, classificação de risco de fraturas ou monitorização terapêutica, devido à presença alterações estruturais', 'Sugerimos não utilizar o fêmur...' );
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(11, 'Sugerimos utilizar o fêmur esquerdo para diagnóstico, classificação de risco de fraturas ou monitorização terapêutica, devido à presença alterações estruturais a direita', 'Sugerimos utilizar o fêmur esquerdo...' );
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(12, 'Sugerimos utilizar o segmento {segmentoSText} para diagnóstico, classificação de risco de fraturas e monitorização terapêutica, devido à presença de alterações nas outras vértebras analisadas', 'Sugerimos utilizar o segmento...');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(13, 'Sugerimos utilizar o segmento {segmentoSText} e o fêmur esquerdo para diagnóstico, classificação de risco de fraturas e monitorização terapêutica, devido à presença de alterações nas outras vértebras analisadas e no fêmur direito', 'Sugerimos utilizar o segmento... e fêmur...');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(14, 'À critério clínico, sugere-se controle com densitometria em {intervaloSNumber} meses', 'À critério clínico, sugere-se controle...');
INSERT INTO LAUDO_COMPARACAO_OPCAO(CODIGO, DESCRICAO, ROTULO) VALUES(15, '{observacaoLText}', 'Observação');

CREATE TABLE MOVIMENTO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	DATA DATE NOT NULL,
	TIPO VARCHAR(7) NOT NULL,
	CONSTRAINT MOVIMENTO_PK PRIMARY KEY (ID),
	CONSTRAINT MOVIMENTO_CHK_TIPO CHECK (TIPO IN ('ENTRADA', 'BAIXA'))
);

CREATE TABLE MOVIMENTO_PRODUTO (
	MOVIMENTO_ID BIGINT NOT NULL,
	PRODUTO_ID BIGINT NOT NULL,
	QUANTIDADE INTEGER DEFAULT 1 NOT NULL,
	CONSTRAINT MOVIMENTO_PRODUTO_FK_MOVIMENTO FOREIGN KEY (MOVIMENTO_ID) REFERENCES MOVIMENTO(ID),
	CONSTRAINT MOVIMENTO_PRODUTO_FK_PRODUTO FOREIGN KEY (PRODUTO_ID) REFERENCES PRODUTO(ID)
);

CREATE TABLE PAGAMENTO_CATEGORIA (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	DESCRICAO VARCHAR(100) NOT NULL,
	CONSTRAINT PAGAMENTO_CATEGORIA_PK PRIMARY KEY (ID)
);

CREATE TABLE PAGAMENTO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	CATEGORIA_ID BIGINT NOT NULL,
	VENCIMENTO DATE NOT NULL,
	DESCRICAO VARCHAR(100) NOT NULL,
	VALOR DECIMAL(8,2) NOT NULL,
	TIPO VARCHAR(7) NOT NULL,
	SITUACAO VARCHAR(8) DEFAULT 'PENDENTE' NOT NULL,
	PAGAMENTO DATE,
	FORMA_PAGAMENTO VARCHAR(27),
	BANCO VARCHAR(50),
	AGENCIA VARCHAR(10),
	CONTA VARCHAR(10),
	CHEQUE VARCHAR(50),
	DOCUMENTO VARCHAR(40),
	CONSTRAINT PAGAMENTO_PK PRIMARY KEY (ID),
	CONSTRAINT PAGAMENTO_FK_PAGAMENTO_CATEGORIA FOREIGN KEY (CATEGORIA_ID) REFERENCES PAGAMENTO_CATEGORIA(ID),
	CONSTRAINT PAGAMENTO_CHK_SITUACAO CHECK (SITUACAO IN ('PENDENTE', 'PAGO')),
	CONSTRAINT PAGAMENTO_CHK_FORMA_PAGAMENTO CHECK (FORMA_PAGAMENTO IN ('DINHEIRO', 'PARCELADO', 'CHEQUE', 'CREDITO', 'DEBITO', 'DEBITO_AUTOMATICO')),
	CONSTRAINT PAGAMENTO_CHK_TIPO CHECK (TIPO IN ('ENTRADA', 'SAIDA'))
);

CREATE TABLE FECHAMENTO (
	ID BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1),
	DATA TIMESTAMP NOT NULL,
	VALOR_DINHEIRO DECIMAL(8,2) NOT NULL,
	VALOR_CARTAO DECIMAL(8,2) NOT NULL,
	VALOR_CHEQUE DECIMAL(8,2) NOT NULL,
	CONSTRAINT FECHAMENTO_PK PRIMARY KEY (ID)
);

CREATE TABLE FECHAMENTO_SAIDA (
	FECHAMENTO_ID BIGINT NOT NULL,
	DESCRICAO VARCHAR(100) NOT NULL,
	VALOR DECIMAL(8,2) NOT NULL,
	CONSTRAINT FECHAMENTO_SAIDA_PK PRIMARY KEY (FECHAMENTO_ID, DESCRICAO),
	CONSTRAINT FECHAMENTO_SAIDA_FK_FECHAMENTO FOREIGN KEY (FECHAMENTO_ID) REFERENCES FECHAMENTO(ID)
);
