## Hands-On MicroProfile 
#Hello "Customer"

In unserer ersten Übung wollen wir uns den typischen Aufbau eines auf  MicroProfile 3.3 basierenden Microservices anschauen, sowie den Umgang mit der Workshop-Infrastruktur kennen lernen. 

##Szenario

Die Fachlichkeit unserer Anwendung setzt sich aus verschiedenen Microservices zusammen. Den Einstieg in die Anwendung und somit eine Art API Gateway stellt der **Customer Service** dar. 

Mit Hilfe des **Customer Service** kann entweder eine Übersicht aller Kunden oder aber die Details eines einzelnen Kunden abgefragt werden. Zu den Detail eines Kunden gehören u.a. die Lieferadresse sowie die Rechnungsadresse des Kunden. Zur Verwaltung der jeweiligenen Detailinformationen sowie der Abbildung der zugehörigen Fachlichkeit stehen dedizierte Services zur Verfügung. 

Der **Billing Services** bietet die Fachlichkeit zur Abrechnung einer Bestellung gegenüber dem Kunden an und verwaltet dazu u.a. die Rechnungsadressen aller Kunden.  

Der **Delivery Service** bietet die Fachlichkeit zur Auslieferung einer Bestellung an den Kunden an und verwaltet dazu u.a. die Lieferadressen aller Kunden. Da bei einer Lieferadresse sichergestellt werden muss, dass es die angegebene Adresse auch tatsächlich gibt, findet beim Anlegen und Ändern einer Lieferadresse eine zusätzliche Validierung statt. Die Aufgabe dieser fachlichen Validierung übernimmt der **Address Validation Service**.

##ToDos

###Step 1: Anwendung starten

Starte die Anwendung, indem du die Anwendung und alle involvierten Services startest: 

``docker-compose up --build``

> Du möchtest wissen, was bei diesem Aufruf hinter den Kullisen passiert? Kein Problem, ein Blick in die zugehörige Docker-Compose Datei *docker-compose.yaml* hilft. 

###Step 1: Use-Cases aufrufen

Rufe die einzelnen Use-Cases der Anwendung auf und versuche anhand der zugrhörigen Sourcen und Log-Ausgaben, die Aufrufe der Microservices untereinander nachzuvollziehen. 

#### Use-Case "Alle Kunden abfragen" 

``GET http://localhost:4000/customers``

#### Use-Case "Details eines Kunden abfragen" 

``GET http://localhost:4000/customers/{customerNumber}``

#### Use-Case "Neuen Kunden anlegen" 

	POST http://localhost:4000/customers
	
	{
		"name":"Hans Dampf", 
		
	}

#### Use-Case "Lieferadresse ändern" 

	PUT http://localhost:4000/customers/0815/delivery-address

	{
		"city":"26122 Oldenburg",
		"recipient":"Martha Dampf",
		"street":
			{
				"name":"Postweg",
				"number":"14"
			}
	}

#### Use-Case "Rechnungsadresse ändern" 

	PUT http://localhost:4000/customers/{customerNumber}/billing-address
	
	{
		"city":"26122 Oldenburg",
		"recipient":"Hans Dampf",
		"street":
			{
				"name":"Postweg",
				"number":"14"
			}
	}
