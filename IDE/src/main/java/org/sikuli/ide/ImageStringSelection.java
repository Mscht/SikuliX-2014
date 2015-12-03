/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sikuli.ide;


import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sikuli.basics.FileManager;
import org.sikuli.basics.Settings;

/**
 *
 * @author Mario
 */
public class ImageStringSelection implements Transferable, ClipboardOwner{
    private boolean _isRepoImage = false;
    private URI data;
    private boolean _onlyAsPath = false;
    public static DataFlavor flavor = new DataFlavor(URI.class, "ImagePath");
    
    public ImageStringSelection(String string, boolean onlyAsPath) {
        _onlyAsPath = onlyAsPath;
        if (string.startsWith("\"")) string = string.substring(1);
        if (string.startsWith(Settings.PROTOCOL_IMAGEREPO)) _isRepoImage = true;
        if (string.endsWith("\"")) string = string.substring(0, string.length()-1);
        try {
            data = new URI(FileManager.slashify(string, false));
        } catch (URISyntaxException ex) {
            Logger.getLogger(ImageStringSelection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean isRepoImage(){
        return _isRepoImage;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        if (_onlyAsPath){
            return new DataFlavor[]{flavor};
        }else{
            return new DataFlavor[]{flavor, DataFlavor.stringFlavor};
        }
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor df) {
        return (df==flavor) ? true : false;
    }

    @Override
    public Object getTransferData(DataFlavor df) {
        if (df==flavor){
            return data;
        } else if(df==DataFlavor.stringFlavor && !_onlyAsPath){
            return "\"" + data.toString() + "\"";
        }
        return null;
    }

    @Override
    public void lostOwnership(Clipboard clpbrd, Transferable t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
