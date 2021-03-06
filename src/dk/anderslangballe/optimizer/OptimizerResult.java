package dk.anderslangballe.optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.anderslangballe.trees.SimpleTree;
import info.aduna.iteration.CloseableIteration;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

public class OptimizerResult {
    private static OptimizerResult _instance;

    public String name;
    public SimpleTree plan;
    public long planningTime;
    public long executionTime;
    public List<Map<String, Object>> tuples;
    public Set<String> bindingNames;
    private String file;

    private OptimizerResult(String name, String file) {
        this.name = name;
        this.file = file;
    }

    public static OptimizerResult getInstance() {
        return OptimizerResult._instance;
    }

    public static OptimizerResult createInstance(String name, String file) {
        // Overwrites instance if there is already one
        _instance = new OptimizerResult(name, file);

        return _instance;
    }

    public void setPlan(SimpleTree tree) throws IOException {
        this.plan = tree;
        this.saveToFile();
    }

    public CloseableIteration<BindingSet, QueryEvaluationException>
        evaluationProxy(Callable<CloseableIteration<BindingSet, QueryEvaluationException>> call) throws Exception {
        long start = System.currentTimeMillis();
        CloseableIteration<BindingSet, QueryEvaluationException> res = call.call();
        List<Map<String, Object>> tuples = new ArrayList<>();

        while (res.hasNext()) {
            BindingSet next = res.next();
            Map<String, Object> tuple = new HashMap<>();
            for (String bindingName : next.getBindingNames()) {
                tuple.put(bindingName, next.getValue(bindingName));
            }
            tuples.add(tuple);
        }

        this.executionTime = System.currentTimeMillis() - start;

        // Get union of binding names
        Set<String> bindingNames = new HashSet<>();
        for (Map<String, Object> tuple : tuples) {
            bindingNames.addAll(tuple.keySet());
        }

        this.tuples = tuples;
        this.bindingNames = bindingNames;
        this.saveToFile();

        return res;
    }

    public void saveToFile() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(new File(file), this);
    }
}
