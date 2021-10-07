package uk.org.nhs.lidoe.medications.HackathonUsercase;


import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.util.Terser;


/* just a wrapper for errors */
public class HL7TerserHelper {

    private Terser _terser;

    public HL7TerserHelper(Terser aTerser) {
        if(aTerser == null)
            throw new IllegalArgumentException("Terser object must be passed in for data retrieval operation");

        _terser = aTerser;
    }

    public String getData(String terserExpression) throws HL7Exception {

        if(terserExpression == null || terserExpression.isEmpty())
            throw new IllegalArgumentException("Terser expression must be supplied for data retrieval operation");

        return _terser.get(terserExpression);
    }

    public void setData(String terserExpression, String value) throws HL7Exception {

        if(terserExpression == null || terserExpression.isEmpty())
            throw new IllegalArgumentException("Terser expression must be supplied for set operation");

        if(value == null) //we will let an empty string still go through
            throw new IllegalArgumentException("Value for set operation must be supplied");

        _terser.set(terserExpression,value);
    }

}
