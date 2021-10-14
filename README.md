# medications_2021_hack


Medication HL7 to FHIR message for use case in NHS Hackathon 19/21 OCtober 2021

Extended to call a standard MLLP server with a HL7 message as part of one of the controllers.  Included a test MLLP server in python based on python-hl7 library.

Just an initial block of stub code to load up some example HL7 messages into FHIR bundles and return them as JSON.  Makes liberal use of https://github.com/LinuxForHealth/hl7v2-fhir-converter
