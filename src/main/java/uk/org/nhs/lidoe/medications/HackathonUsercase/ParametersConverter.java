package uk.org.nhs.lidoe.medications.HackathonUsercase;

import com.fasterxml.jackson.core.ObjectCodec;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;


import org.hl7.fhir.dstu3.model.Parameters;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;

@JsonComponent
public class ParametersConverter {

    private static FhirContext ctx = null;

    public static class Serialize extends JsonSerializer<Parameters> {
        @Override
        public void serialize(Parameters value, JsonGenerator jgen, SerializerProvider provider ) {
            try {
                if ( ctx == null ) {
                    ctx = FhirContext.forDstu3();
                }
                jgen.writeString(ctx.newJsonParser().encodeResourceToString(value));    
            } catch( IOException e) {
                e.printStackTrace();
            }catch( DataFormatException e) {
                e.printStackTrace();
            }
        }
    }
    public static class Deserialize extends JsonDeserializer<Parameters> {
        @Override
        public Parameters deserialize(com.fasterxml.jackson.core.JsonParser jp, DeserializationContext ctxt) throws IOException {
            try {
                if ( ctx == null ) {
                    ctx = FhirContext.forDstu3();
                }
                ObjectCodec oc = jp.getCodec();
                JsonNode node = oc.readTree(jp);         
            //    System.out.println(node.toString());
                Parameters p2 =  ctx.newJsonParser().parseResource(Parameters.class, node.toString());
                return p2;
            }catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    
}

