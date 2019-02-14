package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

/**
 */
public interface PostDefinitionCreateChange {
    public DefinitionChangeLog postCreate(final TestDefinition testDefinition, final Map<String, String[]> extensionFields);
}
