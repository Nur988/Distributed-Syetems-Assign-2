
//Class of Lamport Clock
public class Lamport {

    //declaring the global variable needed to store the Lamport clock value
    private int clockValue = 0;

    public int getClockValue()
    {
        return clockValue;
    }

   //Function used to increment the clock value
    public void incrementClockValue()
    {
        clockValue++;
    }

    // Function used to update and take the Max value
    public void updateClockValue(int value)
    {
        clockValue = Math.max(value,clockValue+1);
    }


}
