package org.reprap.machines;

import javafx.stage.Stage;
import org.reprap.Printer;
import org.reprap.RepRapException;

/**
 * Returns an appropriate Printer object based on the current properties
 */
public class MachineFactory {

    /**
     * Currently this just always assumes we're building
     * a 3-axis cartesian printer.  It should build an
     * appropriate type based on the local configuration.
     * @return new machine
     * @throws Exception
     */
    static public Printer create(Stage stage) throws Exception
    {
        String machine = org.reprap.Preferences.RepRapMachine();

        if (machine.compareToIgnoreCase("GCodeRepRap") == 0){
            return new GCodeRepRap(stage);
        } else if (machine.compareToIgnoreCase("simulator") == 0) {
            return new Simulator(stage);		
        } else {
            throw new RepRapException("Invalid RepRap machine in properties file: " + machine);
        }
    }
	
}
