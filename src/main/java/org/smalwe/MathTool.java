package org.smalwe;

import dev.langchain4j.agent.tool.Tool;

public class MathTool {

    @Tool("Return the sum of two integers")
    public int addNumbers(int a, int b) {
        System.out.println("== [Executing Java Tool: addNumbers with " + a + ", " + b + "] ==");
        return a + b;
    }
}
