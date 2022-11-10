package org.ikasan.dashboard.ui.scheduler.component.validator;

import com.vaadin.flow.data.binder.ErrorMessageProvider;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.StringToLongConverter;

public class StringToDefaultLongConverter extends StringToLongConverter {
    long defaultValue = -1;

    public StringToDefaultLongConverter(String errorMessage, long defaultValue) {
        super(errorMessage);
        this.defaultValue = defaultValue;
    }

    public StringToDefaultLongConverter(Long emptyValue, String errorMessage, long defaultValue) {
        super(emptyValue, errorMessage);
        this.defaultValue = defaultValue;
    }

    public StringToDefaultLongConverter(ErrorMessageProvider errorMessageProvider, long defaultValue) {
        super(errorMessageProvider);
        this.defaultValue = defaultValue;
    }

    public StringToDefaultLongConverter(Long emptyValue, ErrorMessageProvider errorMessageProvider, long defaultValue) {
        super(emptyValue, errorMessageProvider);
        this.defaultValue = defaultValue;
    }

    @Override
    public String convertToPresentation(Long value, ValueContext context) {
        if(value == defaultValue) {
            return "";
        }

        return super.convertToPresentation(value, context);
    }

    @Override
    public Result<Long> convertToModel(String value, ValueContext context) {
        if(value == null) {
            return Result.ok(this.defaultValue);
        }

        return super.convertToModel(value, context);
    }
}
