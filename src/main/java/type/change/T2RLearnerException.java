package type.change;

public class T2RLearnerException extends Exception {

    private String Msg;

    public T2RLearnerException(String msg){
        Msg = msg;
    }

    @Override
    public String toString() {
        return "T2RLearnerException{" +
                "Msg='" + Msg + '\'' +
                '}';
    }
}
