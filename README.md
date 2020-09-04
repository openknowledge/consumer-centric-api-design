##Hands-On MicroProfile 
#JWT Propagation API
  
Die MicroProfile API mit dem etwas komplizierten Namen [JWT RBAC](https://github.com/eclipse/microprofile-jwt-auth) 
hilft dabei, auf Microservices basierende Anwendungen via JWT Token und rollenbasierter Zugriffskontrolle 
(role based access control aka RBAC) abzusichern.   

##Szenario

In unserem Beispiel möchten wir die Möglichkeiten der JWT RBAC API dazu nutzen, den Zugriff auf die verschiedenen 
Use-Cases unserer Anwendung zu beschränken. 

Zur Verwaltung der Nutzer verwenden wir *Keycloak*. Ein Zugriff auf den Keycloak-Server ist nach dessen Start über 
dessen Admin-Console möglich:
                                                    
    http://localhost:9191/auth/admin/
    
    Username: admin
    Password: admin123 

Folgende Nutzer sind initial bereits angelegt und können innerhalb unserer Anwendung verwendet werden:
 
 * admin / admin123 (admin)
 * erika / erika123 (user)
 * max / max123 (user)
 * james / james123 (user)
 
In der folgenden Übung wollen wir den Umgang mit JWT Tokens und der MicroProfile Autorisierung kennenlernen.  
 
##ToDos

Während das Abfragen der Kundenliste für alle angemeldeten Nutzer erlaubt sein soll, dürfen nur Nutzer der Rolle *user* 
die Details einzelner Nutzer einsehen. Das Ändern von Liefer- und/oder Rechnungsadresse wiederum ist nur dem 
jeweiligen Nutzer (Kunden) selbst gestattet. Und auf für das Anlegen einer neuen Kunden gibt es eine Sicherheits-Policy: 
dies darf nur ein Nutzer der Rolle *admin*. 
 
>TIPP: Wie genau die Zugriffs-Policies implementiert werden, zeigt ein Blick in die Klasse *CustomerResource*. 
 
###Step 1: JWT Token "organisieren"
 
Zum Abrufen des JWT Tokens, verwenden wir Keycloak mit dem folgenden Request (am Beispiel 'erika'): 
 
    POST http://localhost:9191/auth/realms/master/protocol/openid-connect/token
    
    Headers: 
        Content-Type: applicatiion/x-www-form-urlencoded
    
    Body:
        grant_type      password
        client_id       onlineshop
        username        erika
        password        erika123
        
###Step 2: JWT Token analysieren

Das in Step 1 zurückgelieferte Token ist Base-46 encoded und daher nicht wirklich gut lesbar. Aber zum Glück 
gibt es mit *JWT.io* eine Web-Seite, die uns das Token im Klartext anzeigt.

Einfach den Base-46 encoded Inhalt des Attributes *access_token* in das Feld "ENCODED" der Web-Seite 
einfügen - schon werden die verschiedenen Claims, wie z.B. *upn* oder *groups*, lesbar. 

###Step 3: Use-Cases aufrufen

Im dritten und letzten Schritt der Übung sollen nun die verschiedenen Use-Cases mit der Tokens der unterschiedlichen 
Nutzer bzw. Rollen aufgerufen werden. 

Der Aufruf der einzelnen Use-Cases erfolgt wie in den vorherigen Beispielen auch. Zusätzlich muss aber das JWT Token als 
Header Information mit angegeben werden (am Beispiel "Liste aller Kunden"): 

    GET http://localhost:4000/customers
    
    Headers: 
        Authorization: Bearer <token>
    
>Was passiert, wenn du keinen Authorization Header angibst? Am besten einfach einmal ausprobieren.  
