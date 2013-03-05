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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProducaoMensal extends JFrame {
	
	private static final String FILE_CATEGORIAS = "categorias.txt";
	
	private static final String FILE_PLANOS = "planos.txt";
	
	private static final String FILE_MEDICOS = "medicos.txt";

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

	private static final int MIN_HEIGHT = 400;

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

	private JProgressBar progressBar;

	private int totalRegistrosLer;
	
	private int registrosLidos;

	private int totalRegistrosGravar;

	private int registrosGravados;

	private JTabbedPane tabbedPane;
	
	private JTextArea textCategorias;

	private static String charset;
	private JTextArea textMedicos;
	private JTextArea textPlanos;

	private Properties planosMap;

	private Properties medicosMap;

	private Workbook workbook;

	private CellStyle dataCellStyle;

	private CellStyle headerCellStyle;

	private CellStyle totalCellStyle;

	private CellStyle totalHeaderStyle;

	private File arquivoDeEntrada;

	private File arquivoDeSaida;

	private File arquivoDeSaidaDir;

	public static void main(String[] args) {
		logger.info("Aplicação iniciada");
		charset = System.getProperty("charset", "ISO-8859-15");
		
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
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		
		textCategorias = new JTextArea(carregaText(FILE_CATEGORIAS));
		tabbedPane.addTab("Categorias", null, new JScrollPane(textCategorias), null);
		
		textPlanos = new JTextArea(carregaText(FILE_PLANOS));
		tabbedPane.addTab("Planos", null, new JScrollPane(textPlanos), null);
		
		textMedicos = new JTextArea(carregaText(FILE_MEDICOS));
		tabbedPane.addTab("Médicos", null, new JScrollPane(textMedicos), null);
		
		progressBar = new JProgressBar();
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		
		btnGerarRelatrio = new JButton("Gerar Relatório");
		btnGerarRelatrio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run() {
						gerarRelatorio();
					}
				}.start();
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
	
	private String carregaText(String filename) {
		try {
			FileInputStream stream = new FileInputStream(filename);
			try {
				FileChannel fc = stream.getChannel();
				MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
				return Charset.forName(charset).decode(bb).toString();
			} finally {
				stream.close();
			}
		} catch (IOException e) {
			error(e);
			return "";
		}
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
						.addComponent(btnGerarRelatrio, Alignment.TRAILING)
						.addComponent(progressBar, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 614, Short.MAX_VALUE)
						.addGroup(Alignment.TRAILING, gl_panel.createSequentialGroup()
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(tabbedPane)))
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
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(tabbedPane)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
					.addGap(12)
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
		
		if (!validaParametros()) {
			return;
		}
		
		caregaArquivoDeDefinicoes(FILE_CATEGORIAS, textCategorias, "categorias");
		caregaArquivoDeDefinicoes(FILE_PLANOS, textPlanos, "planos");
		caregaArquivoDeDefinicoes(FILE_MEDICOS, textMedicos, "medicos");
		
		logger.info("Informações válidas");
		
		carregaDefinicoesDeCategorias();
		
		planosMap = new Properties();
		carregaAliases(planosMap, new BufferedReader(new StringReader(textPlanos.getText())), "planos");

		medicosMap = new Properties();
		carregaAliases(medicosMap, new BufferedReader(new StringReader(textMedicos.getText())), "médicos");
		
		try {
			importarRegistros(arquivoDeEntrada);
			exportarPlanilha(arquivoDeSaida);
		} catch (IOException e) {
			error(e);
		}
	}

	public boolean validaParametros() {
		if (StringUtils.isBlank(textArquivoDeEntrada.getText())) {
			warn("O arquivo de entrada é obrigatório");
			return false;
		}
		
		if (StringUtils.isBlank(textArquivoDeSaida.getText())) {
			warn("O arquivo de saida é obrigatório");
			return false;
		}
		
		arquivoDeEntrada = new File(textArquivoDeEntrada.getText());
		if (!arquivoDeEntrada.exists()) {
			warn("O arquivo de entrada não existe");
			return false;
		}
		if (!arquivoDeEntrada.isFile()) {
			warn("O arquivo de entrada não é um arquivo");
			return false;
		}
		if (!arquivoDeEntrada.canRead()) {
			warn("O arquivo de entrada não está acessível");
			return false;
		}
		
		arquivoDeSaida = new File(textArquivoDeSaida.getText());
		if (arquivoDeSaida.exists()) {
			if (!arquivoDeSaida.isFile()) {
				warn("O arquivo de saida é inválido");
				return false;
			}
			if (!arquivoDeSaida.canWrite()) {
				warn("O arquivo de saida está protegido contra escrita");
				return false;
			}
		}
		
		arquivoDeSaidaDir = arquivoDeSaida.getParentFile();
		if (arquivoDeSaidaDir == null && !arquivoDeSaida.isAbsolute()) {
			arquivoDeSaidaDir = new File(".");
		}
		if (arquivoDeSaidaDir.exists()) {
			if (!arquivoDeSaidaDir.canWrite()) {
				warn("O diretório do arquivo de saida está protegido contra escrita");
				return false;
			}
		} else if (!arquivoDeSaidaDir.mkdirs()) {
			warn("O diretório do arquivo de saida não pôde ser criado");
			return false;
		}
		
		return true;
	}

	private void caregaArquivoDeDefinicoes(String filename, JTextArea textArea, String desc) {
		File file = new File(filename);
		File fileBackup = new File(filename + ".bkp");
		
		if (file.exists()) {
			file.renameTo(fileBackup);
			
			try {
				BufferedWriter writter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
				BufferedReader reader = new BufferedReader(new StringReader(textArea.getText()));
				String linha = null;
				while((linha = reader.readLine()) != null) {
					writter.write(linha + System.getProperty("line.separator"));
				}
				writter.close();
			} catch (IOException e) {
				warn(e);
				fileBackup.renameTo(file);
				textArea.setText(carregaText(filename));
			}
		} else {
			error("O arquivo de configurações de "+ desc +" não foi encontrado. Crie o arquivo com o nome " + filename);
		}
	}

	public void carregaDefinicoesDeCategorias() {
		try {
			logger.info("Lendo definições de categorias");
			BufferedReader categoriasReader = new BufferedReader(new StringReader(textCategorias.getText()));
			String linha = null;
			String categoria = null;
			while((linha = categoriasReader.readLine()) != null) {
				
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
	}
	
	private void carregaAliases(Properties properties, BufferedReader reader, String desc) {
		try {
			String linha = null;
			while ((linha = reader.readLine()) != null) {
				String[] partes = linha.split("=");
				if (partes.length != 2) {
					warn("O arquivo de "+ desc +" está mal formado");
					continue;
				}
				properties.put(partes[0], partes[1]);
			}
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
		
		BufferedReader inputReaderCounter = new BufferedReader(new InputStreamReader(new FileInputStream(arquivoDeEntrada), charset));
		int linhas = 0;
		while(inputReaderCounter.readLine() != null) linhas++;
		inputReaderCounter.close();
		totalRegistrosLer = linhas;
		logger.debug("{} registros para ler", totalRegistrosLer);
		registrosLidos = 0;
		
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(arquivoDeEntrada), charset));
		String line = null;
		while((line = inputReader.readLine()) != null) {
			if (isBlank(line)) continue;
			
			String[] columns = line.split(",");
			String plano = unquote(columns[13]);
			plano = planosMap.getProperty(plano, plano);
			String medico = unquote(columns[14]);
			medico = medicosMap.getProperty(medico, medico);
			String procedimento = unquote(columns[18]);
			String codigo = procedimento.split(" - ")[0];
			
			String categoria = buscaCategoria(no, codigo, 0);
			
			Chave chave = new Chave(plano, medico, categoria);
			acumulaRegistro(chave);
		}
		inputReader.close();
		
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
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				int porcentagemConcluido = (int)((++registrosLidos / (float)totalRegistrosLer) * 50);
				logger.debug("{} registros lidos", registrosLidos);
				logger.debug("leitura {}% conluido", porcentagemConcluido);
				progressBar.setValue(porcentagemConcluido);
			}
		});
	}
	
	private CellStyle buildCellStyle() {
		CellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		return cellStyle;
	}

	public void exportarPlanilha(File arquivoDeSaida) throws FileNotFoundException, IOException {
		logger.info("Gerando relatório");
		
		workbook = new HSSFWorkbook();
		
		dataCellStyle = buildCellStyle();
		
		headerCellStyle = buildCellStyle();
		
		totalHeaderStyle = buildCellStyle();
		totalHeaderStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
		totalHeaderStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		totalCellStyle = buildCellStyle();
		totalCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		totalCellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
		totalCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		totalRegistrosGravar = categorias.size() * medicos.size() * planos.size();
		logger.debug("{} registros para gravar", totalRegistrosGravar);
		registrosGravados = 0;
		
		for (String categoria : categorias.keySet()) {
			Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(categoria));
			
			Row rowPlanos = sheet.createRow(0);
			buildHeaderCell(rowPlanos, (short)0);
			Map<String, AtomicInteger> totaisPlanos = new HashMap<String, AtomicInteger>();
			for (String plano : planos.keySet()) {
				buildHeaderCell(rowPlanos, rowPlanos.getLastCellNum()).setCellValue(plano);
				
				totaisPlanos.put(plano, new AtomicInteger());
			}
			Cell cellTotalHeader = buildTotalHeaderCell(rowPlanos, rowPlanos.getLastCellNum());
			cellTotalHeader.setCellValue("TOTAL");
			
			AtomicInteger totalGeral = new AtomicInteger();
			
			for(String medico : medicos.keySet()) {
				Row row = sheet.createRow(sheet.getLastRowNum() + 1);
				
				Cell cellMedico = buildHeaderCell(row, (short)0);
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
					
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							int porcentagemConcluido = 50 + (int)((++registrosGravados / (float)totalRegistrosGravar) * 50);
							logger.debug("{} registros gravados", registrosGravados);
							logger.debug("escrita {}% conluido", porcentagemConcluido);
							progressBar.setValue(porcentagemConcluido);
						}
					});
				}
				
				Cell cellTotal = buildTotalCell(row, row.getLastCellNum());
				cellTotal.setCellValue(totalMedico.get());
				
				totalGeral.addAndGet(totalMedico.get());
			}
			
			Row rowTotais = sheet.createRow(sheet.getLastRowNum() + 1);
			Cell cellTotalPlanosHeader = buildTotalHeaderCell(rowTotais, (short)0);
			cellTotalPlanosHeader.setCellValue("TOTAL");
			
			for (String plano : planos.keySet()) {
				Cell cellTotal = buildTotalCell(rowTotais, rowTotais.getLastCellNum());
				cellTotal.setCellValue(totaisPlanos.get(plano).get());
			}
			
			Cell cellTotalGeral = buildTotalCell(rowTotais, rowTotais.getLastCellNum());
			cellTotalGeral.setCellValue(totalGeral.get());
			
			for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) { 
				sheet.autoSizeColumn(i);
			}
		}
		
		FileOutputStream fileOut = new FileOutputStream(arquivoDeSaida);
		workbook.write(fileOut);
		fileOut.close();
		
		logger.info("Relatório concluido");
	}

	private Cell buildDataCell(Row row, short column) {
		Cell cell = row.createCell(column);
		cell.setCellStyle(dataCellStyle);
		return cell;
	}
	
	private Cell buildHeaderCell(Row row, short column) {
		Cell cell = row.createCell(column);
		cell.setCellStyle(headerCellStyle);
		return cell;
	}

	private Cell buildTotalCell(Row row, short column) {
		Cell cell = row.createCell(column);
		cell.setCellStyle(totalCellStyle);
		return cell;
	}
	
	private Cell buildTotalHeaderCell(Row row, short column) {
		Cell cell = row.createCell(column);
		cell.setCellStyle(totalHeaderStyle);
		return cell;
	}

	private String buscaCategoria(No no, String codigo, int indice) {
		if (no == null) {
			return codigo;
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
