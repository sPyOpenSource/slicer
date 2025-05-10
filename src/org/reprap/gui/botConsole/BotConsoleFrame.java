/*
 * !!!!!
 * NOTE: PLEASE ONLY EDIT THIS USING THE NETBEANS IDE 6.0.1 OR HIGHER!!!!
 * !!!!!
 * 
 * ... an .xml file is associated with this class. Cheers.
 *
 * BotConsoleFrame.java
 * 
 * Created on 28 March 2008, 08:35
 */

package org.reprap.gui.botConsole;

import org.reprap.Preferences;
import org.reprap.utilities.Debug;

import javax.swing.JOptionPane;

/**
 *
 * @author  Ed Sells, March 2008
 * 
 * Console to operate the RepRap printer manually.
 * 
 */
public class BotConsoleFrame extends javax.swing.JFrame {
    private static final long serialVersionUID = 1L;

    private Thread pollThread = null;
    boolean updatePosition = false;
    private boolean carryOnPolling = true;
    private GenericExtruderTabPanel[] extruderPanels;
    double fractionDone = -1;
    int layer = -1;
    int outOf = -1;
    private static BotConsoleFrame bcf = null;
    private static int exPanelNumber;
    PrintTabFrame printTabFrame;// = new PrintTabFrame();
    
    /** Creates new form BotConsoleFrame */
    public BotConsoleFrame()
    {
        try {
            checkPrefs();
        } catch (Exception e) {
            Debug.e("Failure trying to initialise comms in botConsole: " + e);
            JOptionPane.showMessageDialog(null, e.getMessage());
            return;
        }
        updatePosition = false;
        initComponents();

        this.setTitle("RepRapPro Slicer");

        
        /*
         * Fork off a thread to keep the panels up-to-date
         */
        exPanelNumber = 0;
        pollThread = new Thread() 
        {
            @Override
            public void run()
            {
                Thread.currentThread().setName("GUI Poll");
                while(true)
                {
                    try 
                    {
                        Thread.sleep(1500);
                        updateProgress();
                        if(carryOnPolling)
                            updatePanels();   
                    } catch (InterruptedException ex) {
                        // This is normal when shutting down, so ignore		
                    }
                }
            }
        };

        pollThread.start(); 
        //jTabbedPane1.add(printTabFrame);
        java.awt.EventQueue.invokeLater(() -> {
            printTabFrame.setVisible(true);
        });
    }
    
    public void handleException(Exception e)
    {
    	
    }
    
    /**
     * The update thread calls this to update everything
     * that relies on information from the RepRap machine.
     */
    private void updatePanels()
    {
    	int currentExtruder = org.reprap.Main.gui.getPrinter().getExtruder().getID();
    	
    	try {
            org.reprap.Main.gui.getPrinter().selectExtruder(exPanelNumber, true, false, null);
        } catch (Exception e) {
            handleException(e);
        }
    	try {
            org.reprap.Main.gui.getPrinter().selectExtruder(currentExtruder, true, false, null);
        } catch (Exception e) {
            handleException(e);
        }
    	
    	exPanelNumber++;

    	if(updatePosition){
            xYZTabPanel.recordCurrentPosition();
        }
    	updatePosition = false;
    }
    
    public void getPosition()
    {
    	updatePosition = true;
    }
    
    /**
     * The update thread calls this to update everything
     * that is independent of the RepRap machine.
     * @param fractionDone
     */
    private void updateProgress()
    {
    	printTabFrame.updateProgress(fractionDone, layer, outOf);
    }
    
    public void setFractionDone(double f, int l, int o)
    {
    	if(f >= 0){
            fractionDone = f;
        }
    	if(l >= 0){
            layer = l;
        }
    	if(o >= 0)
            outOf = o;
    }

    /**
     * "Suspend" and "resume" the poll thread.
     * We don't use the actual suspend call (depricated anyway) to
     * prevent resource locking.
     *
     */
    public void suspendPolling()
    {
    	carryOnPolling = false;
    	try 
        {
            Thread.sleep(200); 
        } catch (InterruptedException ex) 
        {
            // This is normal when shutting down, so ignore		
        }
    }
    
    public void resumePolling()
    {
    	try 
        {
            Thread.sleep(200); 
        } catch (InterruptedException ex) 
        {
            // This is normal when shutting down, so ignore		
        }
    	carryOnPolling = true;
    }
 
    private void checkPrefs() throws Exception {
        
        // ID the number of extruder
        extruderCount = Preferences.loadGlobalInt("NumberOfExtruders");
        if (extruderCount < 1)
            throw new Exception("A Reprap printer must contain at least one extruder");
    }
    
    private void initialiseExtruderPanels() {

    }
    
    private void addExtruderPanels() {

    }
       
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        initialiseExtruderPanels();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTabbedPane1.setRequestFocusEnabled(false);

        addExtruderPanels();

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 750, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> {
            bcf = new BotConsoleFrame();
            bcf.setVisible(true);
            bcf.printTabFrame.setConsoleFrame(bcf);
        });
     }
    
    public static BotConsoleFrame getBotConsoleFrame()
    {
    	return bcf;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables
    
    private org.reprap.gui.botConsole.XYZTabPanel xYZTabPanel;
    
    public static PrintTabFrame getPrintTabFrame()
    {
    	return bcf.printTabFrame;
    }
    
    public static XYZTabPanel getXYZTabPanel()
    {
    	return bcf.xYZTabPanel;
    }  
    
    public static GenericExtruderTabPanel getGenericExtruderTabPanel(int i)
    {
    	if(i >= 0 && i < bcf.extruderPanels.length){
            return bcf.extruderPanels[i];
        }
    	Debug.e("getGenericExtruderTabPanel - extruder out of range: " + i);
    	return bcf.extruderPanels[0];
    }
    
    private int extruderCount;	
    
}
