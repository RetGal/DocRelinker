package org.freedom.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.freedom.base.Relinker;

public class MainFrame extends JFrame implements ActionListener {

	private JLabel titleLabel;
	private JPanel mainPanel;
	private JPanel buttonPanel;
	private JPanel p1;
	private JPanel p2;
	private JPanel p3;
	private JPanel p4;
	private JButton b1;
	private JButton b3;
	private JLabel l1;
	private JLabel l2;
	private JLabel l3;
	private JLabel l4;
	private JButton processButton;
	private JFileChooser fileChooser;
	private JFileChooser folderChooser;
	private FileFilter filter;
	private File file;
	private File folder;

	public MainFrame() {

		// set title
		super("DokumentRelinker");
		// borderLayout is the default..
		// setLayout(new BorderLayout());
        
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
		mainPanel = new JPanel();
		p1 = new JPanel();
		p2 = new JPanel();
		p3 = new JPanel();
		p4 = new JPanel();

		mainPanel.setLayout(new GridLayout(4, 1));
		mainPanel.add(p1);
		mainPanel.add(p2);
		mainPanel.add(p3);
		mainPanel.add(p4);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

		b1 = new JButton("Wähle");
		l1 = new JLabel("Dokument");

		p1.add(b1);
		p1.add(l1);

		l2 = new JLabel();
		p2.add(l2);

		b3 = new JButton("Wähle");
		l3 = new JLabel("Zielordner");
		p3.add(b3);
		p3.add(l3);

		l4 = new JLabel();
		p4.add(l4);

		// buttons
		buttonPanel = new JPanel();
		processButton = new JButton("Verarbeite!");
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
			filter = new OdtFilter();
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
				System.out.println("File: " + file.getName());
				// make sure the text fits the window width
				pack();
				titleLabel.setText("und nun den Zielordner..");
			}
			if (folderChooser != null && folderChooser.getSelectedFile() != null) {
				folder = folderChooser.getSelectedFile();

				l4.setText(folder.getAbsolutePath());
				l4.setToolTipText(l4.getText());
				System.out.println("Folder: " + folder.getAbsolutePath());
				// make sure the text fits the window width
				pack();
				if (file != null) {
					titleLabel.setText("Bereit");
				} else {
					titleLabel.setText("Es ist noch kein Dokument ausgewählt");
				}

			}
			retVal = 0;

			// do it
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
						Relinker.process();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						titleLabel.setText("Fertig!");
					}
				}
			}
		}
	}

}
