/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sikuli.ide;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import org.sikuli.basics.PreferencesUser;
import org.sikuli.ide.imagerepo.FileTreePane;

/**
 *
 * @author Mario
 */
public class RepoPane extends JSplitPane{
    
    private class DragMouseListener implements MouseListener, MouseMotionListener{
        Component comp;
        Component leftComp;
        Point dragStartPoint;
        int leftCompStartWidth;
        int compStartWidth;
        int compStartX;
        int mouseStartX;
        boolean isPressed = false;
        
        public DragMouseListener(Component leftComponent, Component rightComponent){
            comp = rightComponent;
            leftComp = leftComponent;
        }
        
        @Override
        public void mouseClicked(MouseEvent me) {}

        @Override
        public void mousePressed(MouseEvent me) {
            isPressed = true;
            dragStartPoint = me.getPoint();
            compStartWidth = comp.getPreferredSize().width;
            compStartX = comp.getLocation().x;
            leftCompStartWidth = leftComp.getSize().width;
            mouseStartX = me.getXOnScreen();
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            isPressed = false;
            dragStartPoint = null;
        }

        @Override
        public void mouseEntered(MouseEvent me) {
            ((Component)me.getSource()).setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
        }

        @Override
        public void mouseExited(MouseEvent me) {
            ((Component)me.getSource()).setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            if (isPressed){
                int deltaX = me.getXOnScreen()-mouseStartX;//dragStartPoint.x - me.getPoint().x;
                int newLeftCompWidth = leftCompStartWidth + deltaX;
                int newCompWidth = compStartWidth - deltaX;
                if ((newLeftCompWidth<200)||(newCompWidth<200)) return;
                Dimension dim = comp.getPreferredSize();
                dim.width = newCompWidth;
                comp.setPreferredSize(dim);
                // Erneuere Layout und zeichne neu
                Component tmp = comp;
                while (tmp!=null){
                    tmp.doLayout();
                    tmp.repaint();
                    tmp = tmp.getParent();  
                }  
            }
        }

        @Override
        public void mouseMoved(MouseEvent me) {}
    };
    private final JPanel splitter;
//    private final FileTree fileTree;
    private final FileTreePane fileTreePane;
    private final ImageThumbPane imageThumbPane;
    private final JScrollPane thumbScrollPane;
//    private final JScrollPane fileTreeScrollPane;
    
    public RepoPane(int splitMode, Component leftComponent) {
        super(splitMode, null, null);
//        fileTree = new FileTree(PreferencesUser.getInstance().getPrefMoreImagesPath());
//        fileTreeScrollPane = new JScrollPane(fileTree);
        fileTreePane = new FileTreePane(PreferencesUser.getInstance().getPrefImageRepoPath());
        
        imageThumbPane = new ImageThumbPane(0, 2);
        imageThumbPane.SetFileTree(fileTreePane.getFileTree());//fileTree);

        thumbScrollPane = new JScrollPane(imageThumbPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        thumbScrollPane.getVerticalScrollBar().setUnitIncrement(imageThumbPane.getThumbHeight()/3);
        
        setLeftComponent(fileTreePane);//fileTreeScrollPane);
        setRightComponent(thumbScrollPane);
        
        splitter = new JPanel();
        splitter.setSize(2, -1);
        //Component paneToLayout = this.getParent().getParent().getParent().getParent();
               
        DragMouseListener ml = new DragMouseListener(leftComponent, this);
        splitter.addMouseListener(ml);
        splitter.addMouseMotionListener(ml);
        //addAncestorListener(new RepoPaneAncestorListener(ml));
    }
    public JPanel getSplitter(){
        return splitter;
    }

    @Override
    public void setPreferredSize(Dimension dmnsn) {
        Dimension thumbDim = imageThumbPane.getSize();
        //Dimension fileTreeDim = fileTree.getSize();
        thumbDim.width = dmnsn.width;
        //thumbDim.height = -1;
        imageThumbPane.setPreferredSize(thumbDim);
        //thumbScrollPane.setPreferredSize(thumbDim);
        super.setPreferredSize(dmnsn);
    }
    
    
}
