package Model.OtherInterface;

import Model.User.User;

public interface Prototype<T> {
    public T deepCopy();
}
