package jmri.enginedriver3;

/**
 * Created by STEVET on 8/4/2014.
 */
public class Route {
    private String _systemName;
    private int _state;
    private String _userName;
    private String _comment;

    public Route(String _systemName, int _state, String _userName, String _comment) {
        this._systemName = _systemName;
        this._state = _state;
        this._userName = _userName;
        this._comment = _comment;
    }
    //return a properly-formatted json request to change this route to new state
    public String getChangeStateJson(int newState) {
        String s = "{\"type\":\"route\",\"data\":{\"name\":\"" + _systemName +
                "\",\"state\":"+ newState + "}}";  //format the json change request
        return s;
    }

    public String getComment() {
        return _comment;
    }
    public void setComment(String comment) {
        this._comment = comment;
    }
    public int getState() {
        return _state;
    }
    public void setState(int state) {
        this._state = state;
    }
//TODO: add setState(String stateDesc)
//TODO: add getStateDesc()

    public String getUserName() {
        return _userName;
    }
    public void setUserName(String userName) {
        this._userName = userName;
    }
    public String getSystemName() {
        return _systemName;
    }
    public void setSystemName(String systemName) {
        this._systemName = systemName;
    }

}