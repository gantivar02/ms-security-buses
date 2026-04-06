export default function LoginMicrosoft({ onSuccess }) {

  const handleMicrosoftLogin = async () => {
    try {
      const response = await msalInstance.loginPopup({
        scopes: ["user.read"],
      });

      const token = response.idToken;

      const res = await fetch("http://localhost:8081/sessions/microsoft", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ token }),
      });

      const data = await res.json();

      if (data.token) {
        onSuccess(data.token); // 🔥 usar contexto
      }

    } catch (error) {
      console.error(error);
    }
  };

  return <button onClick={handleMicrosoftLogin}>Login con Microsoft</button>;
}