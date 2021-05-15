package com.RWdesenv.BlackPinball;

import java.util.List;

import com.badlogic.gdx.physics.box2d.Body;
import com.RWdesenv.BlackPinball.elements.DropTargetGroupElement;
import com.RWdesenv.BlackPinball.elements.FieldElement;
import com.RWdesenv.BlackPinball.elements.FlipperElement;
import com.RWdesenv.BlackPinball.elements.RolloverGroupElement;
import com.RWdesenv.BlackPinball.elements.SensorElement;


/**
 * This class implements the Field.Delegate interface and does nothing for each of the interface
 * methods. Real delegates can subclass this class to avoid having to create empty implementations
 * for events they don't care about. If a field definition doesn't specify a delegate class, an
 * instance of this class will be used as a placeholder delegate.
 */
public class BaseFieldDelegate implements Field.Delegate {

    @Override public void allDropTargetsInGroupHit(Field field, DropTargetGroupElement targetGroup, Ball ball) {}

    @Override public void allRolloversInGroupActivated(Field field, RolloverGroupElement rolloverGroup, Ball ball) {}

    @Override public void flippersActivated(Field field, List<FlipperElement> flippers) {}

    @Override public void processCollision(Field field, FieldElement element, Body hitBody, Ball ball) {}

    @Override public void gameStarted(Field field) {}

    @Override public void ballLost(Field field) {}

    @Override public void gameEnded(Field field) {}

    @Override public void tick(Field field, long nanos) {}

    @Override public void ballInSensorRange(Field field, SensorElement sensor, Ball ball) {}

    @Override public boolean isFieldActive(Field field) {
        return false;
    }
}
