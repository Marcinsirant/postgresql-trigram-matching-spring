package pl.sirant.tm;


import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;

public class CustomPostgreSQLDialect extends PostgreSQLDialect {
  public CustomPostgreSQLDialect() {
    super();
  }

  @Override
  public void initializeFunctionRegistry(FunctionContributions functionContributions) {
    super.initializeFunctionRegistry(functionContributions);
    var functionRegistry = functionContributions.getFunctionRegistry();
    functionRegistry.registerPattern(
        "similarity",
        "SIMILARITY(?1,?2)"
    );
  }

}