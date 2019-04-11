package com.indeed.proctor.common;

import com.indeed.proctor.common.el.LibraryFunctionMapperBuilder;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import org.apache.el.ExpressionFactoryImpl;
import org.apache.log4j.Logger;
import org.apache.taglibs.standard.functions.Functions;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import java.util.Map;

/**
 * A nice tidy packaging of javax.el stuff.
 *
 * @author ketan
 * @author pwp
 *
 */
public class RuleEvaluator {
    private static final Logger LOGGER = Logger.getLogger(RuleEvaluator.class);

    static final FunctionMapper FUNCTION_MAPPER = defaultFunctionMapperBuilder().build();

    static final ExpressionFactory EXPRESSION_FACTORY = new ExpressionFactoryImpl();

    @Nonnull
    final ExpressionFactory expressionFactory;
    @Nonnull
    final CompositeELResolver elResolver;
    @Nonnull
    private final Map<String, ValueExpression> testConstants;
    @Nonnull
    private final FunctionMapper functionMapper;

    RuleEvaluator(
            @Nonnull final ExpressionFactory expressionFactory,
            @Nonnull final FunctionMapper functionMapper,
            @Nonnull final Map<String, Object> testConstantsMap
    ) {
        this.expressionFactory = expressionFactory;

        this.functionMapper = functionMapper;

        elResolver = constructStandardElResolver();

        testConstants = ProctorUtils.convertToValueExpressionMap(expressionFactory, testConstantsMap);
    }

    public static RuleEvaluator createDefaultRuleEvaluator(final Map<String, Object> testConstantsMap) {
        return new RuleEvaluator(EXPRESSION_FACTORY, FUNCTION_MAPPER, testConstantsMap);
    }

    @Nonnull
    private static CompositeELResolver constructStandardElResolver() {
        final CompositeELResolver elResolver = new CompositeELResolver();
        elResolver.add(new ArrayELResolver());
        elResolver.add(new ListELResolver());
        elResolver.add(new BeanELResolver());
        elResolver.add(new MapELResolver());
        return elResolver;
    }


    public static LibraryFunctionMapperBuilder defaultFunctionMapperBuilder() {
        final LibraryFunctionMapperBuilder builder = new LibraryFunctionMapperBuilder()
                                                .add("indeed", ProctorRuleFunctions.class) //backwards compatibility
                                                .add("fn", Functions.class)
                                                .add("proctor", ProctorRuleFunctions.class);
        return builder;
    }

    @Nonnull
    ELContext createELContext(@Nonnull final VariableMapper variableMapper) {
        return new ELContext() {
            @Nonnull
            @Override
            public ELResolver getELResolver() {
                return elResolver;
            }

            @Nonnull
            @Override
            public FunctionMapper getFunctionMapper() {
                return functionMapper;
            }

            @Nonnull
            @Override
            public VariableMapper getVariableMapper() {
                return variableMapper;
            }
        };
    }

    public boolean evaluateBooleanRule(final String rule, @Nonnull final Map<String, Object> values) throws IllegalArgumentException {
        if (ProctorUtils.isEmptyWhitespace(rule)) {
            return true;
        }
        if (!rule.startsWith("${") || !rule.endsWith("}")) {
            LOGGER.error("Invalid rule '" +  rule + "'");   //  TODO: should this be an exception?
            return false;
        }
        final String bareRule = ProctorUtils.removeElExpressionBraces(rule);
        if (ProctorUtils.isEmptyWhitespace(bareRule) || "true".equalsIgnoreCase(bareRule)) {
            return true;    //  always passes
        }
        if ("false".equalsIgnoreCase(bareRule)) {
            return false;
        }

        final Object result = evaluateRule(rule, values, Boolean.class);

        if (result instanceof Boolean) {
            return ((Boolean) result);
        }
        // this should never happen, evaluateRule throws ELException when it cannot coerce to Boolean
        throw new IllegalArgumentException("Received non-boolean return value: "
                + (result == null ? "null" : result.getClass().getCanonicalName())
                + " from rule " + rule);
    }

    /**
     * @return null or a Boolean value representing the expression evaluation result
     * @throws RuntimeException: E.g. PropertyNotFound or other ELException when not of expectedType
     */
    @CheckForNull
    public Object evaluateRule(final String rule, final Map<String, Object> values, final Class expectedType) {
        final Map<String, ValueExpression> localContext = ProctorUtils.convertToValueExpressionMap(expressionFactory, values);
        @SuppressWarnings("unchecked")
        final VariableMapper variableMapper = new MulticontextReadOnlyVariableMapper(testConstants, localContext);
        final ELContext elContext = createELContext(variableMapper);

        final ValueExpression ve = expressionFactory.createValueExpression(elContext, rule, expectedType);
        return ve.getValue(elContext);
    }
}
