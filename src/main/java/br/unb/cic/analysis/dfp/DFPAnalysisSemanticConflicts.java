package br.unb.cic.analysis.dfp;

import br.ufpe.cin.soot.analysis.jimple.JDFP;
import br.unb.cic.analysis.AbstractMergeConflictDefinition;
import br.unb.cic.analysis.StatementsUtil;
import br.unb.cic.soot.graph.*;
import scala.collection.JavaConverters;
import soot.SootMethod;
import soot.Unit;

import java.io.File;
import java.util.*;

/**
 * An analysis wrapper around the Sparse value
 * flow analysis implementation.
 */
public abstract class DFPAnalysisSemanticConflicts extends JDFP {

    private String cp;
    private int depthLimit;
    private StatementsUtil statementsUtils;

    /**
     * DFPAnalysis constructor
     *
     * @param classPath   a classpath to the software under analysis
     * @param definition  a definition with the sources and sinks unities
     * @param depthLimit  the depth limit for the analysis
     * @param entrypoints the list of entry points for the analysis
     */
    public DFPAnalysisSemanticConflicts(String classPath, AbstractMergeConflictDefinition definition, int depthLimit, List<String> entrypoints) {
        this.cp = classPath;
        this.depthLimit = depthLimit;
        this.statementsUtils = new StatementsUtil(definition, entrypoints);
        System.out.println("ESTOU EM DF-INTER");
        System.out.println(classPath);

    }

    public DFPAnalysisSemanticConflicts(String classPath, AbstractMergeConflictDefinition definition) {
        this(classPath, definition, 5, new ArrayList<>());
        System.out.println("ESTOU DF-INTRA");
        System.out.println(classPath);
    }

    public DFPAnalysisSemanticConflicts(String classPath, AbstractMergeConflictDefinition definition, int depthLimit) {
        this(classPath, definition, depthLimit, new ArrayList<>());
    }

    public DFPAnalysisSemanticConflicts(String classPath, AbstractMergeConflictDefinition definition, List<String> entrypoints) {
        this(classPath, definition, 5, entrypoints);
    }

    @Override
    public String sootClassPath() {
        //TODO: what is the role of soot classPath here??
        return cp;
    }

    @Override
    public scala.collection.immutable.List<String> getIncludeList() {
        return JavaConverters.asScalaBuffer(Arrays.asList("")).toList();
    }

    /**
     * Computes the source-sink paths
     * @return a set with a list of nodes that together builds a source-sink path.
     */
    public Set<List<StatementNode>> findSourceSinkPaths() {
        Set<List<StatementNode>> paths = new HashSet<>();

        JavaConverters
                .asJavaCollection(svg().findConflictingPaths())
                .forEach(p -> paths.add(new ArrayList(JavaConverters.asJavaCollection(p))));

       return paths;
    }

    @Override
    public final scala.collection.immutable.List<String> applicationClassPath() {
        String[] array = cp.split(File.pathSeparator);
        return JavaConverters.asScalaBuffer(Arrays.asList(array)).toList();
    }


    @Override
    public final scala.collection.immutable.List<SootMethod> getEntryPoints() {
        return this.statementsUtils.getEntryPoints();
    }

    @Override
    public final NodeType analyze(Unit unit) {
        if(isSource(unit)) {
            return SourceNode.instance();
        }
        else if(isSink(unit)) {
            return SinkNode.instance();
        }
        return SimpleNode.instance();
    }

    protected boolean isSource(Unit unit) {
        return this.statementsUtils.getDefinition().getSourceStatements()
                .stream()
                .map(stmt -> stmt.getUnit())
                .anyMatch(u -> u.equals(unit));
    }

    protected boolean isSink(Unit unit) {
        return this.statementsUtils.getDefinition().getSinkStatements()
                .stream()
                .map(stmt -> stmt.getUnit())
                .anyMatch(u -> u.equals(unit));
    }

    @Override
    public boolean propagateObjectTaint() {
        return true;
    }

    @Override
    public final boolean isFieldSensitiveAnalysis() {
        return true;
    }

    @Override
    public int maxDepth() {
        return this.depthLimit;
    }

    public int getDepthLimit() {
        return this.depthLimit;
    }

    public void setDepthLimit(int depthLimit) {
        this.depthLimit = depthLimit;
    }

    public List<String> reportDFConflicts(){
        Set<List<StatementNode>>  conflicts = findSourceSinkPaths();
        List<String> conflicts_report = new ArrayList<>();
        for (List<StatementNode> conflict: conflicts){
            try{

                StatementNode p1 = conflict.get(0);
                StatementNode p2 = conflict.get(conflict.size()-1);

                System.out.println("DF interference in "+ p1.getPathVisitedMethods().head().getMethod().method());
                System.out.println("Data flows from execution of line "+p1.getPathVisitedMethods().head().line()+" to "+p2.getPathVisitedMethods().head().line()+", defined in "+p1.unit()+" and propagated in "+p2.unit());
                System.out.println("Caused by line "+p1.getPathVisitedMethods().head().line()+ " flow: "+p1.pathVisitedMethodsToString());
                System.out.println("Caused by line "+p2.getPathVisitedMethods().head().line()+ " flow: "+p2.pathVisitedMethodsToString());

                conflicts_report.add("DF interference in "+ p1.getPathVisitedMethods().head().getMethod().method());
                conflicts_report.add("Data flows from execution of line "+p1.getPathVisitedMethods().head().line()+" to "+p2.getPathVisitedMethods().head().line()+", defined in "+p1.unit()+" and propagated in "+p2.unit());
                conflicts_report.add("Caused by line "+p1.getPathVisitedMethods().head().line()+ " flow: "+p1.pathVisitedMethodsToString());
                conflicts_report.add("Caused by line "+p2.getPathVisitedMethods().head().line()+ " flow: "+p2.pathVisitedMethodsToString()+"\n");
            }catch (Exception e){
                System.out.println("Empty list for reporting data flow! Error: "+ e.getMessage());
            }
        }

        return conflicts_report;
    }


}
