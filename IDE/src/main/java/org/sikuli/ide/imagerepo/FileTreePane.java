/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sikuli.ide.imagerepo;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author Mario
 */
public class FileTreePane extends javax.swing.JPanel {
    private String pattern = "";
    private DefaultMutableTreeNode orgNode;
    private String filePath;
    private TagDataBase dbTags = new TagDataBase();
    private IndexingThread indexingThread;
    private SearchPatternKeyListener searchKeyListener = new SearchPatternKeyListener();
    
    private boolean missingDbMsgShown = false;
    
    private final String DB_FILENAME = ".tags";
    
    private class Finder extends SimpleFileVisitor<Path> {

        private final PathMatcher matcher;
        private final ArrayList<Path> matches = new ArrayList<Path>();
        private boolean visitDirectories = true;
        private Path start;

        public Finder(String pattern){
            matcher = FileSystems.getDefault()
                    .getPathMatcher("glob:" + pattern);
        }

        public void setVisitDirectories(boolean visit){
            visitDirectories = visit;
        }

        public void walkFileTree(Path path){
            try {
                start = path;
                // Check files in the start directory
                DirectoryStream<Path> startDirStream = Files.newDirectoryStream(path);
                for (Path p:startDirStream){
                    if(!Files.isDirectory(path)) find(p);
                }
                // Iterate over the tree
                Files.walkFileTree(path, this);
            } catch (IOException ex) {}
        }

        // Compares the glob pattern against
        // the file or directory name.
        void find(Path file) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                matches.add(file);
            }
        }

        // Invoke the pattern matching
        // method on each file.
        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attrs) {
            find(file);
            return CONTINUE;
        }

        // Invoke the pattern matching
        // method on each directory.
        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) {
            if (visitDirectories || dir==start){
                return CONTINUE;
            }else{
                return SKIP_SUBTREE;
            }
        }

        @Override
        public FileVisitResult visitFileFailed(Path file,
                IOException exc) {
            return CONTINUE;
        }
        public ArrayList<Path> getMatches(){
            return matches;
        }
    }
    class SearchPatternKeyListener extends KeyAdapter{
        @Override
        public void keyTyped(KeyEvent ke) {
            super.keyTyped(ke);
            if (indexingThread == null || indexingThread.getState()==Thread.State.TERMINATED || indexingThread.getState()==Thread.State.NEW){
                if (!Files.exists(getDbFilePath())){
                    createIndex();
                    try {
                        indexingThread.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(FileTreePane.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                String filenamePattern = tfFilename.getText();
                String tagsString = tfTags.getText();
                
                if (ke.getComponent() == tfFilename){
                    int start = tfFilename.getSelectionStart();
                    int end = tfFilename.getSelectionEnd();
                    int length = filenamePattern.length();
                    filenamePattern = filenamePattern.substring(0, start) +
                            ke.getKeyChar() +
                            filenamePattern.substring(end, length);
                }
                if (ke.getComponent() == tfTags) tagsString += ke.getKeyChar();
                filenamePattern = filenamePattern.trim();
                tagsString = tagsString.trim();
                filterTreeDb(filenamePattern, tagsString);
                
                //filterTree(tfFilename.getText() + ke.getKeyChar());
            }
        }
        
    }
    
    /*
     * Thread which creates the Index data base.
     */
    class IndexingThread extends Thread{
        private final Path dbIndexPath;
        private final TagDataBase db;
        private final Path indexSrcPath;
        
        /*
         * @param db: The TagDataBase object representing the index
         * @param dbIndexPath: Path to the data base file
         * @param indexSrcPath: Path of the root directory where the image repository is located
        */
        public IndexingThread(TagDataBase db, Path dbIndexPath, Path indexSrcPath){
            this.dbIndexPath = dbIndexPath;
            this.db = db;
            this.indexSrcPath = indexSrcPath;
        }
        
        public boolean isRunning(){
            return (getState() != State.TERMINATED || getState() != State.NEW);
        }
        
        @Override
        public void run() {
            db.setPath(dbIndexPath);
            db.createIndex(indexSrcPath);
        } 
    }
    
    /**
     * Creates new form FileTreePane
     */
    public FileTreePane(String filePath) {
        this.filePath = filePath;
        initComponents();
        ((FileTree)fileTree).setFilePathMatcher(getFilePathMatcher());
        tfFilename.addKeyListener(searchKeyListener);
        tfTags.addKeyListener(searchKeyListener);
        dbTags.setPath(Paths.get(filePath + "\\" + DB_FILENAME));
    }
    
    private DefaultMutableTreeNode getOrCreateNode(DefaultMutableTreeNode parent, String name){
        FileTree.DirectoryTreeNode subPathNode;
        if (parent!=null){
            Enumeration children = parent.children();
            while(children.hasMoreElements()){
                subPathNode = (FileTree.DirectoryTreeNode)children.nextElement();
                if (subPathNode.getName().equals(name)){
                    return subPathNode;
               }
            }
        }
        Path newPath = parent!=null
                    ? ((Path)parent.getUserObject()).resolve(name)
                    : Paths.get(name);
        FileTree.DirectoryTreeNode node = new FileTree.DirectoryTreeNode(newPath);
        if (parent!=null) parent.add(node);
        return node;
    }
        
    /**
     *
     * @return
     */
    public PathMatcher getFilePathMatcher(){
        return FileSystems.getDefault()
                    .getPathMatcher("glob:" + (pattern.equals("")
                                                ? "*"
                                                : pattern));
    }
    private void filterTreeDb(String filenamePattern, String tagsString){
        // Prepare tags and start the search
        String[] tags = tagsString.split(",");
        if (tags.length==1 && "".equals(tags[0])) tags = new String[]{};
        
        //get a copy
        if (orgNode==null){
            orgNode = (DefaultMutableTreeNode)fileTree.getModel().getRoot();
        }
        // if no filter is active restore
        if (filenamePattern.trim().equals("")){
            boolean tagsValid = false;
            for (String t:tags){
                if(!t.trim().equals("")){
                    tagsValid = true;
                    break;
                }
            }
            if (!tagsValid){
                if (fileTree.getModel().getRoot()!=orgNode)
                    fileTree.setModel(new DefaultTreeModel(orgNode));
                return;
            }
            filenamePattern = "*";
        }
        
        // check wether the index db exists, if not print a warning
        if (missingDbMsgShown==false && !Files.exists(Paths.get(filePath + "\\" + DB_FILENAME))){
            System.out.println("Index database does not exist. Please create the index first.");
            missingDbMsgShown = true;
            return;
        }
        
        // start the search
        ArrayList<String> foundFiles = dbTags.search(filenamePattern, tags);
        // restore orgNode if no pattern
        if (foundFiles.isEmpty()){
            fileTree.setModel(new DefaultTreeModel(null));
//            if (fileTree.getModel().getRoot()!=orgNode){
//                fileTree.setModel(new DefaultTreeModel(orgNode));
//            }
            return;
        }
        
        // create a sub tree of the search results
        DefaultMutableTreeNode newNode;
        newNode = getOrCreateNode(null, ((FileTree)fileTree).getRootPath());
        for (String file: foundFiles){
            Path relPath = ((Path)orgNode.getUserObject()).relativize(Paths.get(file));
            Iterator<Path> iter = relPath.iterator();
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)newNode;
            Path tmpPath = (Path)orgNode.getUserObject();
            while(iter.hasNext()){
                Path subPath = iter.next();
                tmpPath = tmpPath.resolve(subPath);
                if(!Files.isDirectory(tmpPath)){
                    ((FileTree.DirectoryTreeNode)currentNode).addFilterName(subPath.toString());
                    break;
                }
                currentNode = getOrCreateNode(currentNode, subPath.toString());
            }
        }
        // set new model
        fileTree.setModel(new DefaultTreeModel(newNode));
    }
    private void filterTree(String text) {
        
        pattern = text.trim();
        ((FileTree)fileTree).setFilePathMatcher(getFilePathMatcher());
        
        //get a copy
        if (orgNode==null){
            orgNode = (DefaultMutableTreeNode)fileTree.getModel().getRoot();
        }
        // restore orgNode if no pattern
        if (pattern.equals("")){
            if (fileTree.getModel().getRoot()!=orgNode){
                fileTree.setModel(new DefaultTreeModel(orgNode));
            }
            return;
        }

        Finder finder = new Finder(pattern);
        DefaultMutableTreeNode newNode;
        try{
            Files.walkFileTree((Path)orgNode.getUserObject(), finder);
            newNode = getOrCreateNode(null, ((FileTree)fileTree).getRootPath());
            for (Path file: finder.getMatches()){
                Path relPath = ((Path)orgNode.getUserObject()).relativize(file);
                Iterator<Path> iter = relPath.iterator();
                DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)newNode;
                Path tmpPath = (Path)orgNode.getUserObject();
                while(iter.hasNext()){
                    Path subPath = iter.next();
                    tmpPath = tmpPath.resolve(subPath);
                    if(!Files.isDirectory(tmpPath)) break;
                    currentNode = getOrCreateNode(currentNode, subPath.toString());
                }
            }
            fileTree.setModel(new DefaultTreeModel(newNode));
        } catch (IOException ex) {fileTree.setModel(new DefaultTreeModel(null));}

    }

    public FileTree getFileTree() {
        return (FileTree)fileTree;
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        btnSearch = new javax.swing.JButton();
        btnExpand = new javax.swing.JButton();
        btnCollapse = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        lFilename = new javax.swing.JLabel();
        lTags = new javax.swing.JLabel();
        tfFilename = new javax.swing.JTextField();
        tfTags = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        fileTree = new FileTree(filePath);
        btnIndex = new javax.swing.JButton();

        setMinimumSize(new java.awt.Dimension(420, 300));
        setPreferredSize(new java.awt.Dimension(200, 200));
        setLayout(new java.awt.GridBagLayout());

        btnSearch.setText("Search");
        btnSearch.setBorderPainted(false);
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 0, 0);
        add(btnSearch, gridBagConstraints);

        btnExpand.setText("Expand");
        btnExpand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExpandActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        add(btnExpand, gridBagConstraints);

        btnCollapse.setText("Collapse");
        btnCollapse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCollapseActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        add(btnCollapse, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 3, 0);
        add(jSeparator1, gridBagConstraints);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        add(jSeparator2, gridBagConstraints);

        lFilename.setLabelFor(tfFilename);
        lFilename.setText("Filename:");
        lFilename.setVisible(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        add(lFilename, gridBagConstraints);

        lTags.setLabelFor(tfTags);
        lTags.setText("Tags:");
        lTags.setVisible(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        add(lTags, gridBagConstraints);

        tfFilename.setVisible(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        add(tfFilename, gridBagConstraints);

        tfTags.setVisible(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        add(tfTags, gridBagConstraints);

        jScrollPane1.setViewportView(fileTree);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        add(jScrollPane1, gridBagConstraints);

        btnIndex.setText("Make Index");
        btnIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIndexActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        add(btnIndex, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        boolean isVisible = lFilename.isVisible();
        boolean visible = !isVisible;
        lFilename.setVisible(visible);
        tfFilename.setVisible(visible);
        lTags.setVisible(visible);
        tfTags.setVisible(visible);
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnExpandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExpandActionPerformed
        ((FileTree)fileTree).expandAll();
        
    }//GEN-LAST:event_btnExpandActionPerformed

    private void btnCollapseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCollapseActionPerformed
        for (int i = fileTree.getRowCount() - 1; i >= 0; i--) {
         fileTree.collapseRow(i);
        }
    }//GEN-LAST:event_btnCollapseActionPerformed
    
    private Path getDbFilePath(){
        return Paths.get(filePath + "\\" + DB_FILENAME);
    }
    private void btnIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIndexActionPerformed
        createIndex();
    }//GEN-LAST:event_btnIndexActionPerformed
    private void createIndex(){
        if (indexingThread == null || indexingThread.getState()==Thread.State.TERMINATED){
            indexingThread = new IndexingThread(dbTags,
                    getDbFilePath(),
                    Paths.get(filePath));
            indexingThread.start();
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCollapse;
    private javax.swing.JButton btnExpand;
    private javax.swing.JButton btnIndex;
    private javax.swing.JButton btnSearch;
    private javax.swing.JTree fileTree;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JLabel lFilename;
    private javax.swing.JLabel lTags;
    private javax.swing.JTextField tfFilename;
    private javax.swing.JTextField tfTags;
    // End of variables declaration//GEN-END:variables
}
