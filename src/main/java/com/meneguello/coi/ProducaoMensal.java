package com.meneguello.coi;

import static java.awt.BorderLayout.CENTER;
import static javax.swing.JFileChooser.APPROVE_SELECTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.trim;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProducaoMensal extends JFrame {
	
	private class Chave {

		public final String plano;
		public final String medico;
		public final String categoria;

		public Chave(String plano, String medico, String categoria) {
			this.plano = plano;
			this.medico = medico;
			this.categoria = categoria;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((categoria == null) ? 0 : categoria.hashCode());
			result = prime * result
					+ ((medico == null) ? 0 : medico.hashCode());
			result = prime * result + ((plano == null) ? 0 : plano.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Chave other = (Chave) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (categoria == null) {
				if (other.categoria != null)
					return false;
			} else if (!categoria.equals(other.categoria))
				return false;
			if (medico == null) {
				if (other.medico != null)
					return false;
			} else if (!medico.equals(other.medico))
				return false;
			if (plano == null) {
				if (other.plano != null)
					return false;
			} else if (!plano.equals(other.plano))
				return false;
			return true;
		}

		private ProducaoMensal getOuterType() {
			return ProducaoMensal.this;
		}

		@Override
		public String toString() {
			StrBuilder sb = new StrBuilder();
			sb.append(plano)
			       .append(", ")
			       .append(medico)
			       .append(", ")
			       .append(categoria);
			return sb.toString();
		}		
		
	}
	
	private class No {
		public Map<Character, No> filhos = new HashMap<Character, No>();
		public String categoria;
	}
	
	static {
		PropertyConfigurator.configure("log4j.properties");
	}
	
	private static final Logger logger = LoggerFactory.getLogger(ProducaoMensal.class);

	private static final long serialVersionUID = 1L;

	private static final String PROP_WINDOW_X = "window.x";
	
	private static final String PROP_WINDOW_Y = "window.y";
	
	private static final String PROP_WINDOW_W = "window.w";
	
	private static final String PROP_WINDOW_H = "window.h";
	
	private static final String PROP_INPUT_FILE = "input.file";
	
	private static final String PROP_OUTPUT_FILE = "output.file";

	private static final int MIN_HEIGHT = 170;

	private static final int MIN_WIDTH = 650;
	
	private JPanel contentPane;
	
	private JPanel panel;
	
	private JTextField textArquivoDeEntrada;
	
	private JTextField textArquivoDeSaida;
	
	private JButton btnArquivoDeEntrada;
	
	private JButton btnArquivoDeSaida;
	
	private JButton btnGerarRelatrio;

	private Preferences preferencias;
	
	private No no = new No();

	private Map<String, Set<Chave>> planos;

	private Map<String, Set<Chave>> medicos;

	private Map<String, Set<Chave>> categorias;

	private Map<Chave, AtomicInteger> contadores;

	private JFileChooser arquivoDeEntradaFileChooser;

	private JFileChooser arquivoDeSaidaFileChooser;

	public static void main(String[] args) {
		logger.info("Aplicação iniciada");
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					ProducaoMensal frame = new ProducaoMensal();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public ProducaoMensal() {
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent evt) {
				int x = ProducaoMensal.this.getX();
				int y = ProducaoMensal.this.getY();
				logger.debug("janela movida para ({},{})", x, y);
				
				preferencias.putInt(PROP_WINDOW_X, x);
				preferencias.putInt(PROP_WINDOW_Y, y);
				flushPreferencias();
			}
			
			@Override
			public void componentResized(ComponentEvent evt) {
				int w = ProducaoMensal.this.getWidth();
				int h = ProducaoMensal.this.getHeight();
				logger.debug("janela redimensionada para {}x{}", w, h);
				
				preferencias.putInt(PROP_WINDOW_W, w);
				preferencias.putInt(PROP_WINDOW_H, h);
				flushPreferencias();
			}
		});
		
		try {
			logger.info("Lendo definições de categorias");
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(new FileInputStream("categorias.txt"), "ISO-8859-15"));
			String linha = null;
			String categoria = null;
			while((linha = inputReader.readLine()) != null) {
				
				linha = trim(linha);
				if (linha.endsWith(":")) {
					categoria = linha.substring(0, linha.length() - 1);
					logger.debug("categoria: {}", categoria);
				} else {
					logger.debug("definição: {}", linha);
					adicionaCategoria(no, categoria, linha, 0);
				}
			}
			logger.info("Definições de categoria carregadas");
		} catch(IOException e) {
			error(e);
		}
		
		preferencias = Preferences.userNodeForPackage(ProducaoMensal.class);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = preferencias.getInt(PROP_WINDOW_X, dim.width / 2 - MIN_WIDTH / 2);
		int y = preferencias.getInt(PROP_WINDOW_Y, dim.height / 2 - MIN_HEIGHT / 2);
		int w = preferencias.getInt(PROP_WINDOW_W, MIN_WIDTH);
		int h = preferencias.getInt(PROP_WINDOW_H, MIN_HEIGHT);
		logger.debug("Geometria da janela: ({},{}) {}x{}", x, y, w, h);
		
		preferencias.putInt(PROP_WINDOW_X, x);
		preferencias.putInt(PROP_WINDOW_Y, y);
		preferencias.putInt(PROP_WINDOW_W, w);
		preferencias.putInt(PROP_WINDOW_H, h);
		flushPreferencias();
		
		setTitle("Gerador de Relatório de Produção Mensal");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBounds(x, y, w, h);
		setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		panel = new JPanel();
		contentPane.add(panel, CENTER);
		
		textArquivoDeEntrada = new JTextField(preferencias.get(PROP_INPUT_FILE, null));
		textArquivoDeEntrada.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				preferencias.put(PROP_INPUT_FILE, textArquivoDeEntrada.getText());
				flushPreferencias();
			}
		});
		
		btnArquivoDeEntrada = new JButton("Arquivo de Entrada");
		btnArquivoDeEntrada.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selecionarArquivoDeEntrada();
			}
		});
		
		textArquivoDeSaida = new JTextField(preferencias.get(PROP_OUTPUT_FILE, null));
		textArquivoDeSaida.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				preferencias.put(PROP_OUTPUT_FILE, textArquivoDeSaida.getText());
				flushPreferencias();
			}
		});
		
		btnArquivoDeSaida = new JButton("Arquivo de Saida");
		btnArquivoDeSaida.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selecionarArquivoDeSaida();
			}
		});
		
		btnGerarRelatrio = new JButton("Gerar Relatório");
		btnGerarRelatrio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				gerarRelatorio();
			}
		});
		
		configuraLayout();
		
		arquivoDeEntradaFileChooser = new JFileChooser();
		arquivoDeEntradaFileChooser.setFileSelectionMode(FILES_ONLY);
		arquivoDeEntradaFileChooser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (APPROVE_SELECTION.equals(e.getActionCommand())) {
					confirmarSelecaoArquivoDeEntrada();
				}
			}
		});
		
		arquivoDeSaidaFileChooser = new JFileChooser();
		arquivoDeSaidaFileChooser.setFileSelectionMode(FILES_ONLY);
		arquivoDeSaidaFileChooser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (APPROVE_SELECTION.equals(e.getActionCommand())) {
					confirmarSelecaoArquivoDeSaida();
				}
			}
		});
	}
	
	private void configuraLayout() {
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel.createSequentialGroup()
							.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
								.addComponent(textArquivoDeEntrada, GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
								.addComponent(textArquivoDeSaida, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING, false)
								.addComponent(btnArquivoDeSaida, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(btnArquivoDeEntrada, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
						.addComponent(btnGerarRelatrio, Alignment.TRAILING))
					.addContainerGap())
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addComponent(btnArquivoDeEntrada)
						.addComponent(textArquivoDeEntrada, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
					.addGap(12)
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addComponent(textArquivoDeSaida, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnArquivoDeSaida))
					.addPreferredGap(ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
					.addComponent(btnGerarRelatrio)
					.addContainerGap())
		);
		panel.setLayout(gl_panel);
	}
	
	private void adicionaCategoria(No no, String categoria, String linha, int indice) {
		if (indice >= linha.length() || linha.charAt(indice) == '*') {
			no.categoria = categoria;
		} else if (!Character.isDigit(linha.charAt(indice))) {
			adicionaCategoria(no, categoria, linha, indice + 1);
		} else {
			No filho = no.filhos.get(linha.charAt(indice));
			if (filho == null) {
				filho = new No();
				no.filhos.put(linha.charAt(indice), filho);
			}
			adicionaCategoria(filho, categoria, linha, indice + 1);
		}
	}

	public void selecionarArquivoDeEntrada() {
		logger.info("Selecionando o arquivo de entrada");
		arquivoDeEntradaFileChooser.showOpenDialog(this);
	}
	
	private void confirmarSelecaoArquivoDeEntrada() {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					String filename = arquivoDeEntradaFileChooser.getSelectedFile().getCanonicalPath();
					logger.info("arquivo de entrada selecionado %s", filename);
					textArquivoDeEntrada.setText(filename);
					preferencias.put(PROP_INPUT_FILE, filename);
					flushPreferencias();
				} catch (IOException e) {
					error("Falha na leitura do arquivo de entrada");
				}
			}
		});
	}

	public void selecionarArquivoDeSaida() {
		logger.info("Selecionando o arquivo de entrada");
		arquivoDeSaidaFileChooser.showSaveDialog(this);
	}

	private void confirmarSelecaoArquivoDeSaida() {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					String filename = arquivoDeSaidaFileChooser.getSelectedFile().getCanonicalPath();
					logger.info("arquivo de saida selecionado %s", filename);
					textArquivoDeSaida.setText(filename);
					preferencias.put(PROP_OUTPUT_FILE, filename);
					flushPreferencias();
				} catch (IOException e) {
					error("Falha na escolha do arquivo de saida");
				}
			}
		});
	}

	public void gerarRelatorio() {
		logger.info("Validando as informações");
		
		if (StringUtils.isBlank(textArquivoDeEntrada.getText())) {
			warn("O arquivo de entrada é obrigatório");
			return;
		}
		
		if (StringUtils.isBlank(textArquivoDeSaida.getText())) {
			warn("O arquivo de saida é obrigatório");
			return;
		}
		
		File arquivoDeEntrada = new File(textArquivoDeEntrada.getText());
		if (!arquivoDeEntrada.exists()) {
			warn("O arquivo de entrada não existe");
			return;
		}
		if (!arquivoDeEntrada.isFile()) {
			warn("O arquivo de entrada não é um arquivo");
			return;
		}
		if (!arquivoDeEntrada.canRead()) {
			warn("O arquivo de entrada não está acessível");
			return;
		}
		
		File arquivoDeSaida = new File(textArquivoDeSaida.getText());
		if (arquivoDeSaida.exists()) {
			if (!arquivoDeSaida.isFile()) {
				warn("O arquivo de saida é inválido");
				return;
			}
			if (!arquivoDeSaida.canWrite()) {
				warn("O arquivo de saida está protegido contra escrita");
				return;
			}
		}
		
		File arquivoDeSaidaDir = arquivoDeSaida.getParentFile();
		if (arquivoDeSaidaDir == null && !arquivoDeSaida.isAbsolute()) {
			arquivoDeSaidaDir = new File(".");
		}
		if (arquivoDeSaidaDir.exists()) {
			if (!arquivoDeSaidaDir.canWrite()) {
				warn("O diretório do arquivo de saida está protegido contra escrita");
			}
		} else if (!arquivoDeSaidaDir.mkdirs()) {
			warn("O diretório do arquivo de saida não pôde ser criado");
			return;
		}
		
		logger.info("Informações válidas");
		
		try {
			importarRegistros(arquivoDeEntrada);
			exportarPlanilha(arquivoDeSaida);
		} catch (IOException e) {
			error(e);
		}
	}

	public void importarRegistros(File arquivoDeEntrada) throws IOException {
		logger.info("Importando registros");
		
		planos = new TreeMap<String, Set<Chave>>();
		medicos = new TreeMap<String, Set<Chave>>();
		categorias = new TreeMap<String, Set<Chave>>();
		contadores = new HashMap<Chave, AtomicInteger>();
		
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(arquivoDeEntrada), "ISO-8859-15"));
		String line = null;
		while((line = inputReader.readLine()) != null) {
			if (isBlank(line)) continue;
			
			String[] columns = line.split(",");
			String plano = unquote(columns[13]);
			String medico = unquote(columns[14]);
			String procedimento = unquote(columns[18]);
			String codigo = procedimento.split(" - ")[0];
			
			String categoria = buscaCategoria(no, codigo, 0);
			
			Chave chave = new Chave(plano, medico, categoria);
			acumulaRegistro(chave);
		}
		
		logger.info("Registros importados");
	}

	private void acumulaRegistro(Chave chave) {
		AtomicInteger contador = contadores.get(chave);
		if (contador == null) {
			contador = new AtomicInteger();
			contadores.put(chave, contador);
		}
		contador.incrementAndGet();
		
		Set<Chave> planoList = planos.get(chave.plano);
		if (planoList == null) {
			planoList = new HashSet<Chave>();
			planos.put(chave.plano, planoList);
		}
		planoList.add(chave);

		Set<Chave> medicoList = medicos.get(chave.medico);
		if (medicoList == null) {
			medicoList = new HashSet<Chave>();
			medicos.put(chave.medico, medicoList);
		}
		medicoList.add(chave);

		Set<Chave> categoriaList = categorias.get(chave.categoria);
		if (categoriaList == null) {
			categoriaList = new HashSet<Chave>();
			categorias.put(chave.categoria, categoriaList);
		}
		categoriaList.add(chave);
	}

	public void exportarPlanilha(File arquivoDeSaida) throws FileNotFoundException, IOException {
		logger.info("Gerando relatório");
		
		Workbook wb = new HSSFWorkbook();

		for (String categoria : categorias.keySet()) {
			Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(categoria));
			
			Row rowPlanos = sheet.createRow(0);
			rowPlanos.createCell(0);
			Map<String, AtomicInteger> totaisPlanos = new HashMap<String, AtomicInteger>();
			for (String plano : planos.keySet()) {
				buildHeaderCell(rowPlanos, rowPlanos.getLastCellNum()).setCellValue(plano);
				
				totaisPlanos.put(plano, new AtomicInteger());
			}
			Cell cellTotalHeader = buildHeaderCell(rowPlanos, rowPlanos.getLastCellNum());
			cellTotalHeader.setCellValue("TOTAL");
			
			AtomicInteger totalGeral = new AtomicInteger();
			
			for(String medico : medicos.keySet()) {
				Row row = sheet.createRow(sheet.getLastRowNum() + 1);
				
				Cell cellMedico = row.createCell(0);
				cellMedico.setCellValue(medico);
				
				AtomicInteger totalMedico = new AtomicInteger();
				
				for (String plano : planos.keySet()) {
					Chave chave = new Chave(plano, medico, categoria);
					AtomicInteger contador = contadores.get(chave);
					
					Cell cell = buildDataCell(row, row.getLastCellNum());
					if (contador != null) {
						cell.setCellValue(contador.get());
						totalMedico.addAndGet(contador.get());
						totaisPlanos.get(plano).addAndGet(contador.get());
					} else {
						cell.setCellValue("*");
					}
				}
				
				Cell cellTotal = buildDataCell(row, row.getLastCellNum());
				cellTotal.setCellValue(totalMedico.get());
				
				totalGeral.addAndGet(totalMedico.get());
			}
			
			Row rowTotais = sheet.createRow(sheet.getLastRowNum() + 1);
			Cell cellTotalPlanosHeader = rowTotais.createCell(0);
			cellTotalPlanosHeader.setCellValue("TOTAL");
			
			for (String plano : planos.keySet()) {
				Cell cellTotal = buildDataCell(rowTotais, rowTotais.getLastCellNum());
				cellTotal.setCellValue(totaisPlanos.get(plano).get());
			}
			
			Cell cellTotalGeral = buildDataCell(rowTotais, rowTotais.getLastCellNum());
			cellTotalGeral.setCellValue(totalGeral.get());
			
			for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) { 
				sheet.autoSizeColumn(i);
			}
		}
		
		FileOutputStream fileOut = new FileOutputStream(arquivoDeSaida);
		wb.write(fileOut);
		fileOut.close();
		
		logger.info("Relatório concluido");
	}

	public Cell buildDataCell(Row row, short column) {
		return row.createCell(column);
	}
	
	public Cell buildHeaderCell(Row rowPlanos, short column) {
		return rowPlanos.createCell(column);
	}

	private String buscaCategoria(No no, String codigo, int indice) {
		if (no == null) {
			return "CATEGORIA DESCONHECIDA";
		}
		if (no.categoria != null) {
			return no.categoria;
		}
		if (!Character.isDigit(codigo.charAt(indice))) {
			return buscaCategoria(no, codigo, indice + 1);
		}
		return buscaCategoria(no.filhos.get(codigo.charAt(indice)), codigo, indice + 1);
	}
	
	private String unquote(String value) {
		return value.replaceAll("(^\")|(\"$)","");
	}

	private void flushPreferencias() {
		try {
			preferencias.flush();
			logger.debug("preferências armazenadas");
		} catch (BackingStoreException e) {
			warn(e);
		}
	}

	private void warn(String message) {
		JOptionPane.showMessageDialog(this, message, "Validação", WARNING_MESSAGE);		
	}
	
	private void warn(Exception e) {
		warn(e.getMessage());		
	}

	private void error(String message) {
		JOptionPane.showMessageDialog(this, message, "Falha", ERROR_MESSAGE);
		System.exit(1);
	}

	private void error(Exception e) {
		error(e.getMessage());
	}
		
}
