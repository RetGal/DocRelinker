package org.freedom.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileFilter;

import org.freedom.log.Log;
import org.freedom.base.Relinker;

class MainFrame extends JFrame implements ActionListener, ComponentListener {

	private final JLabel titleLabel;
	private final JButton b1;
	private final JButton b3;
	private final JLabel l2;
	private final JLabel l4;
	private final JButton processButton;
	private JFileChooser fileChooser;
	private JFileChooser folderChooser;
	private File file;
	private File folder;

	static final int WIDTH = 560;
	static final int HEIGHT = 280;

	static final int MIN_WIDTH = 560;
	static final int MIN_HEIGHT = 280;

	public MainFrame() {

		// set title
		super("DokumentRelinker");
		// borderLayout is the default..
		// setLayout(new BorderLayout());

		setSize(WIDTH, HEIGHT);
		addComponentListener(this);

		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			// if Nimbus is not available, we have to stick to the default l&f
		}

		// label
		titleLabel = new JLabel("Wähle das zu exportierende Dokument..");
		titleLabel.setHorizontalAlignment(JLabel.CENTER);
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// main
		JPanel mainPanel = new JPanel();
		JPanel p1 = new JPanel();
		JPanel p2 = new JPanel();
		JPanel p3 = new JPanel();
		JPanel p4 = new JPanel();

		mainPanel.setLayout(new GridLayout(4, 1));
		mainPanel.add(p1);
		mainPanel.add(p2);
		mainPanel.add(p3);
		mainPanel.add(p4);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

		b1 = new JButton("\u2609 Dokument");
		JLabel l1 = new JLabel("");

		p1.add(b1);
		p1.add(l1);

		l2 = new JLabel();
		p2.add(l2);

		b3 = new JButton("\u2609 Zielordner");
		JLabel l3 = new JLabel("");
		p3.add(b3);
		p3.add(l3);

		l4 = new JLabel();
		p4.add(l4);

		// buttons
		JPanel buttonPanel = new JPanel();
		processButton = new JButton("\u2699 Verarbeite");
		buttonPanel.add(processButton);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

		// top level
		add(titleLabel, BorderLayout.NORTH);
		add(mainPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		pack();

		// listeners
		b1.addActionListener(this);
		b3.addActionListener(this);
		processButton.addActionListener(this);

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8099143503680007336L;

	@Override
	public void actionPerformed(ActionEvent event) {

		int retVal = 0;

		// master document selection
		if (event.getSource() == b1) {

			fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Dokument");
			FileFilter filter = new OdtFilter();
			fileChooser.setFileFilter(filter);
			filter = new DocxFilter();
			fileChooser.setFileFilter(filter);
			fileChooser.setAcceptAllFileFilterUsed(false);
			retVal = fileChooser.showOpenDialog(null);
		}
		// target path selection
		if (event.getSource() == b3) {
			folderChooser = new JFileChooser();
			// folderChooser.setCurrentDirectory(new java.io.File("."));
			folderChooser.setDialogTitle("Zielordner");
			folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			folderChooser.setAcceptAllFileFilterUsed(false);
			retVal = folderChooser.showOpenDialog(null);
		}

		if (retVal == JFileChooser.APPROVE_OPTION) {
			if (fileChooser != null && fileChooser.getSelectedFile() != null) {
				file = fileChooser.getSelectedFile();

				l2.setText(file.getName());
				l2.setToolTipText(l2.getText());
				Log.info("File: " + file.getName());
				titleLabel.setText("und nun den Zielordner..");
			}
			if (folderChooser != null && folderChooser.getSelectedFile() != null) {
				folder = folderChooser.getSelectedFile();

				if (file != null) {
					if (file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator))
							.equals(folder.getAbsolutePath())) {
						titleLabel.setText("Dokument darf sich nicht im Zielordner befinden");
						folder = null;
					} else {
						l4.setText(folder.getAbsolutePath());
						l4.setToolTipText(l4.getText());
						Log.info("Folder: " + folder.getAbsolutePath());
						titleLabel.setText("Bereit");
					}
				} else {
					titleLabel.setText("Es ist noch kein Dokument ausgewählt");
				}

			}
			retVal = 0;

			// do it
			int copied = 0;
			if (event.getSource() == processButton) {
				if (file == null) {
					titleLabel.setText("Welches Dokument?");
				} else if (folder == null) {
					titleLabel.setText("Bitte Zielordner wählen");
				} else {
					titleLabel.setText("Verarbeitung läuft..");
					try {
						Relinker.setMainDocument(file);
						Relinker.setTargetDirectory(folder);
						copied = Relinker.process();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						titleLabel.setText("Fertig ("+copied+ " verlinkte Dateien kopiert)");
					}
				}
			}
		}
	}

	@Override
	public void componentResized(ComponentEvent e) {

		int width = getWidth();
		int height = getHeight();

		boolean resize = false;
		// check if either width or height are below minimum
		if (width < MIN_WIDTH) {
			resize = true;
			width = MIN_WIDTH;
		}
		if (height < MIN_HEIGHT) {
			resize = true;
			height = MIN_HEIGHT;
		}
		if (resize) {
			setSize(width, height);
		}
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	public static void main(String args[]) {
		MainFrame f = new MainFrame();
		f.setVisible(true);
	}

}
