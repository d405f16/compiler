import SymbolTable.StoreValue;
import SymbolTable.Symbol;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.InvalidClassException;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JBCodeGenerator extends chawkBaseVisitor {
    private Map<String, StoreValue> variableMap = new LinkedHashMap<String, StoreValue>();
    private int label=-1;
    private int store=0;

    @Override
    public Object visit(ParseTree tree) {
        if (tree == null) {
            return null;
        }
        return super.visit(tree);
    }

    @Override
    public Object visitProgram(chawkParser.ProgramContext ctx) {
        System.out.println("public class cHawk {");
        for (int i = 0; i < ctx.getChildCount(); i++) {
            Object child = visit(ctx.getChild(i));
            if (child != null)
                System.out.println(child);
        }
        System.out.println("}");
        return null;
    }

    @Override
    public Object visitBody(chawkParser.BodyContext ctx) {
        String line = "";
        for (int i = 0; i < ctx.getChildCount(); i++) {
            Object child = visit(ctx.getChild(i));
            if (child != null) line += child;
        }
        return line;
    }

    @Override
    public Object visitStatement(chawkParser.StatementContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Object visitWhileStatement(chawkParser.WhileStatementContext ctx) {
        String line = "";
        line += visit(ctx.expression());
        String type = ctx.expression().getChild(0).getClass().getSimpleName().equals("Variable_expression_Context")
                ? variableMap.get(ctx.expression().getChild(0).getText()).getType() : visit(ctx.expression().getChild(2)).getClass().getSimpleName();
        System.out.println(ctx.expression().getChild(1).getText());
        if (type.equals("Float")) {
            line += "f";
            line += boolOperator(ctx.expression().getChild(1).getText());
            line += " Label" + labelinc() + "\r\n";
        }
        else {
            line += "l";
            line += boolOperator(ctx.expression().getChild(1).getText());
            line += " Label" + labelinc() + "\r\n";
        }
        visitBody(ctx.body());
        return line;
    }

    private String boolOperator(String Op){
        switch(Op){
            case "<": return "cmpg";
            default : return null;
        }
    }
    @Override
    public Object visitRelationalExpression(chawkParser.RelationalExpressionContext ctx) {
        String line = "";
        StoreValue variable1 = variableMap.get(ctx.expression(0).getText());
        StoreValue variable2 = variableMap.get(ctx.expression(1).getText());
        String type1 = (variable1 != null) ? variable1.getType() : visit(ctx.expression(0)).getClass().getSimpleName();
        String type2 = (variable2 != null) ? variable2.getType() : visit(ctx.expression(1)).getClass().getSimpleName();

        if((type1.contains("Integer") && type2.contains("Integer")) || (type1.contains("Float") && type2.contains("Float"))) {
            if (ctx.expression(0).getClass().getSimpleName().contains("Variable_expression_Context")) {
                StoreValue variable = variableMap.get(ctx.expression(0).getText());
                line += variableSwitch(variable);
            }
            if (ctx.expression(0).getClass().getSimpleName().contains("ValueExpressionContext")) {
                String type = visit(ctx.expression(0)).getClass().getSimpleName();
                line += constantSwitch(type, ctx.expression(0).getText());
            }
            if (ctx.expression(1).getClass().getSimpleName().contains("Variable_expression_Context")) {
                StoreValue variable = variableMap.get(ctx.expression(1).getText());
                line += variableSwitch(variable);
            }
            if (ctx.expression(1).getClass().getSimpleName().contains("ValueExpressionContext")) {
                String type = visit(ctx.expression(1)).getClass().getSimpleName();
                line += constantSwitch(type, ctx.expression(1).getText());
            }
            return line;
        }
        else throw new ArithmeticException("Both numbers must be of same type");
    }
    private String constantSwitch(String type, String value){
        switch(type){
            case "Integer" : return "bipush " + value + "\r\n";
            case "Float" : return "ldc " + value + "\r\n";
            default : return null;
        }
    }
    private String variableSwitch(StoreValue variable){
        switch (variable.getType()){
            case "Integer" :return "iload " + variable.getLocation() + "\r\n";
            case "Float" : return "fload " + variable.getLocation()  + "\r\n";
            case "String" : return "aload " + variable.getLocation() + "\r\n";
            default : return null;
        }
    }

    @Override
    public Object visitVariableStatement(chawkParser.VariableStatementContext ctx) {
        String type = visit(ctx.getChild(2)).getClass().getSimpleName();
        String name = ctx.getChild(0).getText();
        String line = "";
        switch (type){
            case "Integer" : line += "bipush " + visit(ctx.getChild(2)) + "\r\n" +
            "istore " + storeinc() + "\r\n";
                break;
            case "Float" : line += "ldc " + visit(ctx.getChild(2)) + "\r\n" +
                    "fstore " + storeinc() + "\r\n";
                break;
            case "String" : line += "ldc " + visit(ctx.getChild(2)) + "\r\n" +
                    "astore " + storeinc() + "\r\n";
        }
        StoreValue variable = new StoreValue(name,type,store);
        variableMap.put(name, variable);
        return line;
    }

    @Override
    public Object visitValueExpression(chawkParser.ValueExpressionContext ctx) {
        String value = ctx.getText();

        if (value.equals("true") || value.equals("false")) {
            return Boolean.parseBoolean(value);
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                try {
                    return Float.parseFloat(value);
                } catch (NumberFormatException f) {
                    return value;
                }
            }
        }
    }

    private int labelinc() {
        label++;
        return label;
    }
    private int storeinc(){
        store++;
        return store;
    }
}