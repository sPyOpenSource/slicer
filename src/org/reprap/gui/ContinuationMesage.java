package org.reprap.gui;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class ContinuationMesage extends javax.swing.JDialog {
	private static final long serialVersionUID = 1L;
	private static Point lastScreenPosition = null;
	private JButton cancelButton;

	private JTextField message;
	private JButton okButton;
	
	private boolean result; ///< True if continue was selected, otherwise false

	public ContinuationMesage(JFrame frame, String message) { //, 
		super(frame);
		initGUI(message);
		
		if (lastScreenPosition != null)
			setLocation(lastScreenPosition);		
	}
	
	private void initGUI(String messageContent) {
		try {
			{
				okButton = new JButton();
				getContentPane().add(okButton);
				okButton.setText("Continue...");
				okButton.setBounds(175, 98, 105, 28);
				okButton.setMnemonic(java.awt.event.KeyEvent.VK_ENTER);
				okButton.addActionListener((ActionEvent evt) -> {
                                    okButtonActionPerformed(evt);
                                });
			}
			{
				message = new JTextField();
				getContentPane().add(message);
				message.setBounds(0, 0, 308, 35);
				message.setOpaque(false);
				message.setEditable(false);
				message.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
				message.setHorizontalAlignment(SwingConstants.CENTER);
				message.setText(messageContent);
			}
			{
				cancelButton = new JButton();
				getContentPane().add(cancelButton);
				cancelButton.setText("Cancel");
				cancelButton.setBounds(35, 98, 105, 28);
				cancelButton.addActionListener((ActionEvent evt) -> {
                                    cancelButtonActionPerformed(evt);
                                });
			}
			{
				getContentPane().setLayout(null);
				this.setTitle("Progress pause");
			}
			this.setSize(316, 158);
			this.getRootPane().setDefaultButton(okButton);
		} catch (Exception e) {
			Logger.getLogger(ContinuationMesage.class.getName()).log(Level.SEVERE, null, e);
		}
	}
	
	private void okButtonActionPerformed(ActionEvent evt) {
		lastScreenPosition = getLocation();
		result = true;
		synchronized(this) {
			notify();
		}
	}
	
	private void cancelButtonActionPerformed(ActionEvent evt) {
		lastScreenPosition = getLocation();
		result = false;
		synchronized(this) {
			notify();
		}
	}
	
	public boolean getResult() {
		return result;
	}

}
