package com.itstwoam.aoeqol.jei;

import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;

/** Recipe handler for recipe objects that are their own JEI wrapper. */
final class SimpleHandler<T extends IRecipeWrapper> implements IRecipeHandler<T> {
    private final Class<T> type;
    private final String uid;

    SimpleHandler(Class<T> type, String uid) {
        this.type = type;
        this.uid = uid;
    }

    @Override public Class<T> getRecipeClass() { return type; }
    @Override public String getRecipeCategoryUid() { return uid; }
    @Override public String getRecipeCategoryUid(T recipe) { return uid; }
    @Override public IRecipeWrapper getRecipeWrapper(T recipe) { return recipe; }
    @Override public boolean isRecipeValid(T recipe) { return true; }
}
