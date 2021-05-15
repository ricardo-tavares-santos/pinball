package com.RWdesenv.BlackPinball;

public interface IDrawable {
    void draw(Field field, IFieldRenderer renderer);
    int getLayer();
}
