package com.meneguello.coi;

import static javax.swing.JFileChooser.APPROVE_SELECTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;


public class ProducaoMensal extends JFrame {

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
	
	private JTextField textArquivoDeEntrada;
	
	private JTextField textArquivoDeSaida;

	private Preferences preferences;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
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

	/**
	 * Create the frame.
	 */
	public ProducaoMensal() {
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent evt) {
				preferences.putInt(PROP_WINDOW_X, ProducaoMensal.this.getX());
				preferences.putInt(PROP_WINDOW_Y, ProducaoMensal.this.getY());
				flushPreferences();
			}
			
			@Override
			public void componentResized(ComponentEvent evt) {
				preferences.putInt(PROP_WINDOW_W, ProducaoMensal.this.getWidth());
				preferences.putInt(PROP_WINDOW_H, ProducaoMensal.this.getHeight());
				flushPreferences();
			}
		});
		
		preferences = Preferences.userNodeForPackage(ProducaoMensal.class);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = preferences.getInt(PROP_WINDOW_X, dim.width / 2 - MIN_WIDTH / 2);
		preferences.putInt(PROP_WINDOW_X, x);
		int y = preferences.getInt(PROP_WINDOW_Y, dim.height / 2 - MIN_HEIGHT / 2);
		preferences.putInt(PROP_WINDOW_Y, y);
		int w = preferences.getInt(PROP_WINDOW_W, MIN_WIDTH);
		preferences.putInt(PROP_WINDOW_W, w);
		int h = preferences.getInt(PROP_WINDOW_H, MIN_HEIGHT);
		preferences.putInt(PROP_WINDOW_H, h);
		flushPreferences();
		
		setTitle("Gerador de Relatório de Produção Mensal");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(x, y, w, h);
		setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.CENTER);
		
		textArquivoDeEntrada = new JTextField(preferences.get(PROP_INPUT_FILE, null));
		textArquivoDeEntrada.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				preferences.put(PROP_INPUT_FILE, textArquivoDeEntrada.getText());
				flushPreferences();
			}
		});
		textArquivoDeEntrada.setColumns(10);
		
		JButton btnArquivoDeEntrada = new JButton("Arquivo de Entrada");
		btnArquivoDeEntrada.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selecionarArquivoDeEntrada();
			}
		});
		
		textArquivoDeSaida = new JTextField(preferences.get(PROP_OUTPUT_FILE, null));
		textArquivoDeSaida.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				preferences.put(PROP_OUTPUT_FILE, textArquivoDeSaida.getText());
				flushPreferences();
			}
		});
		textArquivoDeSaida.setColumns(10);
		
		JButton btnArquivoDeSaida = new JButton("Arquivo de Saida");
		btnArquivoDeSaida.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selecionarArquivoDeSaida();
			}
		});
		
		JButton btnGerarRelatrio = new JButton("Gerar Relatório");
		btnGerarRelatrio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				gerarRelatorio();
			}
		});
		
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
	
	public void selecionarArquivoDeSaida() {
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(FILES_ONLY);
		fileChooser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (APPROVE_SELECTION.equals(e.getActionCommand())) {
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							try {
								String filename = fileChooser.getSelectedFile().getCanonicalPath();
								textArquivoDeSaida.setText(filename);
								preferences.put(PROP_OUTPUT_FILE, filename);
								flushPreferences();
							} catch (IOException e) {
								error("Falha na escolha do arquivo de saida");
							}
						}
					});
				}
			}
		});
		fileChooser.showSaveDialog(ProducaoMensal.this);
	}

	public void selecionarArquivoDeEntrada() {
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(FILES_ONLY);
		fileChooser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (APPROVE_SELECTION.equals(e.getActionCommand())) {
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							try {
								String filename = fileChooser.getSelectedFile().getCanonicalPath();
								textArquivoDeEntrada.setText(filename);
								preferences.put(PROP_INPUT_FILE, filename);
								flushPreferences();
							} catch (IOException e) {
								error("Falha na leitura do arquivo de entrada");
							}
						}
					});
				}
			}
		});
		fileChooser.showOpenDialog(ProducaoMensal.this);
	}

	public void gerarRelatorio() {
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
	}

	private void flushPreferences() {
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			//TODO: Logging
		}
	}

	private void warn(String message) {
		JOptionPane.showMessageDialog(this, message, "Validação", WARNING_MESSAGE);		
	}

	private void error(String message) {
		JOptionPane.showMessageDialog(this, message, "Falha", ERROR_MESSAGE);	
	}
		
}
