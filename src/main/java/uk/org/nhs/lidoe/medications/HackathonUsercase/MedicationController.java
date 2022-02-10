package uk.org.nhs.lidoe.medications.HackathonUsercase;


import java.util.concurrent.atomic.AtomicLong;

import javax.naming.NameNotFoundException;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.fhir.exception.FHIRException;
import com.ibm.fhir.model.generator.FHIRAbstractGenerator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.MediaType;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
// import org.hl7.fhir.r4.model.Bundle;
// import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
// import org.hl7.fhir.r4.model.Binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Map;

// HAPI HL7 V2 imports
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.Initiator;

import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

import org.hl7.fhir.convertors.conv30_40.Bundle30_40;
import org.hl7.fhir.r4.model.Bundle;


import ca.uhn.fhir.context.FhirContext;

@RestController
public class MedicationController {


    private static final int PORT_NUMBER = 2575;// change this to whatever your port number is
    private static final String HL7_MLLP_HOST = "localhost";// change this to whatever your port number is
    private static HapiContext context = new DefaultHapiContext();

    // take a FHIR message and verify it 
    //  note we cannot parse the HL7 message directly in the @RequestBody because it is not json/xml
    // so hence the String and parsing inside of the function based on HAPI HL7 system.
    
    @RequestMapping(value="/fhir/**",
                  headers="Accept=*/*",
                  method = { RequestMethod.POST },
                  consumes = {MediaType.APPLICATION_JSON_VALUE},
                  produces = {MediaType.APPLICATION_JSON_VALUE} )
    @ResponseBody
    public String processFHIRRequest(
        @RequestParam Map<String, String> reqParam,
        @RequestHeader Map<String, String> reqHeaders, 
        @RequestBody org.hl7.fhir.r4.model.Bundle fhirInput,
        HttpServletRequest request) 
            throws IOException, NameNotFoundException {
                try {
                    //FhirContext ctx = FhirContext.forDstu3();
                    
                     FhirContext ctx= FhirContext.forR4();
                    String json = ctx.newJsonParser().encodeResourceToString(fhirInput);    
                    return json;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "whoops" ;
        }  
       

    @RequestMapping(value="/fhir3/**",
        headers="Accept=*/*",
        method = { RequestMethod.POST },
        consumes = {MediaType.APPLICATION_JSON_VALUE},
        produces = {MediaType.APPLICATION_JSON_VALUE} )
    @ResponseBody
    public String processFHIR3Request(
    @RequestParam Map<String, String> reqParam,
    @RequestHeader Map<String, String> reqHeaders, 
    @RequestBody org.hl7.fhir.dstu3.model.Bundle fhirInput3,
    HttpServletRequest request) 
    throws IOException, NameNotFoundException {
        try {
          //FhirContext ctx = FhirContext.forDstu3();
          
          FhirContext ctx= FhirContext.forR4();

          // Convert the incoming R3 bundle probably from GP Connect to R4.
         org.hl7.fhir.r4.model.Bundle b4 = Bundle30_40.convertBundle(fhirInput3);
        
          String json = ctx.newJsonParser().encodeResourceToString(b4);    


          return json;
      } catch (Exception e) {
          e.printStackTrace();
      }
      return "whoops" ;
}     
        // public String toJSON3() {
        
        //     FhirContext ctx3 = FhirContext.forDstu3();
        //     org.hl7.fhir.dstu3.model.Bundle b3 = Bundle10_30.convertBundle(bMaternity);
    
        //     return ctx3.newJsonParser().encodeResourceToString(b3);
        // }

// take a HL7 message and send it on to a local MLLP server.
    // okay this follows the specification of https://hapifhir.github.io/hapi-hl7v2/hapi-hl7overhttp/specification.html
    //  note we cannot parse the HL7 message directly in the @RequestBody because it is not json/xml
    // so hence the String and parsing inside of the function based on HAPI HL7 system.
    
    @RequestMapping(value="/mllp/**",
                  headers="Accept=*/*",
                  method = { RequestMethod.POST },
                  consumes = {" x-application/hl7-v2+er7"},
                   produces = { "application/hl7-v2+er7" })
    @ResponseBody
    public String processHL7RequestMLLP(
        @RequestParam Map<String, String> reqParam,
        @RequestHeader Map<String, String> reqHeaders, 
        @RequestBody String strHL7,
        HttpServletRequest request)
            throws IOException, NameNotFoundException {
                Message hl7AckMessage=null;
                Message hl7Message = null;
               // System.out.println("Request: " + strHL7.replace("\n","\r"));         
        
                try {
                    PipeParser pipeParser = new PipeParser();
                    // AF message from HL7 rest API may have the wrong carriage return force it to the correct one or parser fails !
                    hl7Message = pipeParser.parse(strHL7.replace("\n","\r"));
                                    

                    Terser terser = new Terser(hl7Message);

                    HL7TerserHelper terserhelper = new HL7TerserHelper(terser);
                    try {
                        String data = terserhelper.getData("/.MSH-6");
                        System.out.println("Field 6 of MSH, the destination is :" + data);
                        data = terserhelper.getData("/.MSH-9");
                        System.out.println("Field 9 of MSH, the destination is :" + data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // example of this code comes from https://saravanansubramanian.com/hl72xhapisendmessage/
                    // create a new MLLP client over the specified port
                    Connection connection = context.newClient(HL7_MLLP_HOST, PORT_NUMBER, false);
                    
                    // The initiator which will be used to transmit our message
                    Initiator initiator = connection.getInitiator();

                    hl7AckMessage = initiator.sendAndReceive(hl7Message);
                    //  was constructing ACK message by hand for a little while before finding this.
                    //hl7AckMessage = hl7Message.generateACK();
            //        this.kafkaProducer.saveString( hl7Message.toString() );
                } catch (Exception e) {
                    e.printStackTrace();
                }
               
       
        return hl7AckMessage.toString() ;
    }

    // okay this follows the specification of https://hapifhir.github.io/hapi-hl7v2/hapi-hl7overhttp/specification.html
    //  note we cannot parse the HL7 message directly in the @RequestBody because it is not json/xml
    // so hence the String and parsing inside of the function based on HAPI HL7 system.
    // Note we dont really do anything with the message !
    @RequestMapping(value="/convert/**",
                  headers="Accept=*/*",
                  method = { RequestMethod.POST },
                  consumes = {" x-application/hl7-v2+er7"},
                   produces = { "application/json" })
    @ResponseBody
    public String processHL7RequestConvert(
        @RequestParam Map<String, String> reqParam,
        @RequestHeader Map<String, String> reqHeaders, 
        @RequestBody String strHL7,
        HttpServletRequest request)
            throws IOException, NameNotFoundException {
                Message hl7AckMessage=null;
                Message hl7Message = null;
                String outputFHIR = null;
               // System.out.println("Request: " + strHL7.replace("\n","\r"));         
        
                try {
                    PipeParser pipeParser = new PipeParser();
                    // AF message from HL7 rest API may have the wrong carriage return force it to the correct one or parser fails !
                    hl7Message = pipeParser.parse(strHL7.replace("\n","\r"));
                                    

                    Terser terser = new Terser(hl7Message);

                    HL7TerserHelper terserhelper = new HL7TerserHelper(terser);
                    try {
                        String data = terserhelper.getData("/.MSH-6");
                        System.out.println("Field 6 of MSH, the destination is :" + data);
                        data = terserhelper.getData("/.MSH-9");
                        System.out.println("Field 9 of MSH, the destination is :" + data);

                        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
                        outputFHIR= ftv.convert(strHL7.replace("\n","\r")); // generated a FHIR output
                        System.out.println(outputFHIR);

                        // // AF this gives us the fifth OBX item (ordered from 0) 5 component and 5th subcomponent 1^2^3^4^5^ !
                        // //    not sure if this message will be hardwired like this in all situations but assuming static 
                        // //    for now.  NB XML FHIR message !
                        // String encodedFHIR = terserhelper.getData("/OBX(4)-5-5");
                        // // no validation 
                        // byte[] decodedFHIR = Base64.getDecoder().decode(encodedFHIR);
                        // String fhirMsg = new String(decodedFHIR);
                        // System.out.println("Field 5 subcomponent 5 of OBX is :" + fhirMsg);

                        // Just going to place this XML message on the standard kafka queue for now.  This actually makes no sense.
              //          this.kafkaProducer.saveString(fhirMsg);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //  was constructing ACK message by hand for a little while before finding this.
                    hl7AckMessage = hl7Message.generateACK();
            //        this.kafkaProducer.saveString( hl7Message.toString() );
                } catch (Exception e) {
                    e.printStackTrace();
                }
               
       
        return outputFHIR ;
    }

        // okay this follows the specification of https://hapifhir.github.io/hapi-hl7v2/hapi-hl7overhttp/specification.html
    //  note we cannot parse the HL7 message directly in the @RequestBody because it is not json/xml
    // so hence the String and parsing inside of the function based on HAPI HL7 system.
    // Note we dont really do anything with the message !
    @RequestMapping(value="/hl7/**",
                  headers="Accept=*/*",
                  method = { RequestMethod.POST },
                  consumes = {" x-application/hl7-v2+er7"},
                   produces = { "x-application/hl7-v2+er7" })
    @ResponseBody
    public String processHL7Request(
        @RequestParam Map<String, String> reqParam,
        @RequestHeader Map<String, String> reqHeaders, 
        @RequestBody String strHL7,
        HttpServletRequest request)
            throws IOException, NameNotFoundException {
                Message hl7AckMessage=null;
                Message hl7Message = null;

               // System.out.println("Request: " + strHL7.replace("\n","\r"));
                
                try {
                    PipeParser pipeParser = new PipeParser();
                    // AF message from HL7 rest API may have the wrong carriage return force it to the correct one or parser fails !
                    hl7Message = pipeParser.parse(strHL7.replace("\n","\r"));
   //                 this.kafkaProducer.toString();   
                    

                    Terser terser = new Terser(hl7Message);

                    HL7TerserHelper terserhelper = new HL7TerserHelper(terser);
                    try {
                        String data = terserhelper.getData("/.MSH-6");
                        System.out.println("Field 6 of MSH, the destination is :" + data);
                        data = terserhelper.getData("/.MSH-9");
                        System.out.println("Field 9 of MSH, the destination is :" + data);

                                   // AF this gives us the fifth OBX item (ordered from 0) 5 component and 5th subcomponent 1^2^3^4^5^ !
                        //    not sure if this message will be hardwired like this in all situations but assuming static 
                        //    for now.  NB XML FHIR message !
          
                        // Just going to place this XML message on the standard kafka queue for now.  This actually makes no sense.
  //                      this.kafkaProducer.saveString(fhirMsg);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //  was constructing ACK message by hand for a little while before finding this.
                    hl7AckMessage = hl7Message.generateACK();
            //        this.kafkaProducer.saveString( hl7Message.toString() );
                } catch (Exception e) {
                    e.printStackTrace();
                }
               
       
        return hl7AckMessage.toString() ;
    }



    // okay this follows the specification of https://hapifhir.github.io/hapi-hl7v2/hapi-hl7overhttp/specification.html
    //  note we cannot parse the HL7 message directly in the @RequestBody because it is not json/xml
    // so hence the String and parsing inside of the function based on HAPI HL7 system.
    // Note we dont really do anything with the message !
    @RequestMapping(value="/strata/**",
                  headers="Accept=*/*",
                  method = { RequestMethod.POST },
                  consumes = {" x-application/hl7-v2+er7"},
                   produces = { "x-application/hl7-v2+er7" })
    @ResponseBody
    public String processHL7StrataRequest(
        @RequestParam Map<String, String> reqParam,
        @RequestHeader Map<String, String> reqHeaders, 
        @RequestBody String strHL7,
        HttpServletRequest request)
            throws IOException, NameNotFoundException {
                Message hl7AckMessage=null;
                Message hl7Message = null;

               // System.out.println("Request: " + strHL7.replace("\n","\r"));
                
                try {
                    PipeParser pipeParser = new PipeParser();
                    // AF message from HL7 rest API may have the wrong carriage return force it to the correct one or parser fails !
                    hl7Message = pipeParser.parse(strHL7.replace("\n","\r"));
   //                 this.kafkaProducer.toString();   
                    

                    Terser terser = new Terser(hl7Message);

                    HL7TerserHelper terserhelper = new HL7TerserHelper(terser);
                    try {
                        String data = terserhelper.getData("/.MSH-6");
                        System.out.println("Field 6 of MSH, the destination is :" + data);
                        data = terserhelper.getData("/.MSH-9");
                        System.out.println("Field 9 of MSH, the destination is :" + data);

                                   // AF this gives us the fifth OBX item (ordered from 0) 5 component and 5th subcomponent 1^2^3^4^5^ !
                        //    not sure if this message will be hardwired like this in all situations but assuming static 
                        //    for now.  NB XML FHIR message !
                        String encodedFHIR = terserhelper.getData("/OBX(4)-5-5");
                        // no validation 
                        byte[] decodedFHIR = Base64.getDecoder().decode(encodedFHIR);
                        String fhirMsg = new String(decodedFHIR);
                        System.out.println("Field 5 subcomponent 5 of OBX is :" + fhirMsg);

                        // Just going to place this XML message on the standard kafka queue for now.  This actually makes no sense.
  //                      this.kafkaProducer.saveString(fhirMsg);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //  was constructing ACK message by hand for a little while before finding this.
                    hl7AckMessage = hl7Message.generateACK();
            //        this.kafkaProducer.saveString( hl7Message.toString() );
                } catch (Exception e) {
                    e.printStackTrace();
                }
               
       
        return hl7AckMessage.toString() ;
    }

}

