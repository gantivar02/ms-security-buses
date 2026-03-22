import { PublicClientApplication } from "@azure/msal-browser";
import { msalConfig } from "../auth/msalConfig";

const msalInstance = new PublicClientApplication(msalConfig);

export default function LoginMicrosoft() {

  const login = async () => {
    try {
      // 🔥 INICIALIZAR MSAL (CLAVE)
      await msalInstance.initialize();

      const response = await msalInstance.loginPopup({
        scopes: ["openid", "profile", "email", "User.Read"]
      });

      const idToken = response.idToken;

      if (!idToken) {
        console.error("No se recibió idToken");
        return;
      }

      const res = await fetch("http://localhost:8081/sessions/microsoft", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ token: idToken })
      });

      const data = await res.json();

      console.log("Respuesta backend:", data);

      if (data.token) {
        localStorage.setItem("token", data.token);
      }

    } catch (error) {
      console.error("Error login Microsoft:", error);
    }
  };

  return (
    <button onClick={login}>
      🔷 Iniciar sesión con Microsoft
    </button>
  );
}