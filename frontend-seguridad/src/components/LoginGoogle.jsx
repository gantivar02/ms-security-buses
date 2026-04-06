import { GoogleLogin } from '@react-oauth/google';

export default function LoginGoogle({ onSuccess }) {

  const handleSuccess = async (credentialResponse) => {
    const token = credentialResponse.credential;

    console.log("GOOGLE TOKEN:", token);

    // Enviar al backend
    const res = await fetch("http://localhost:8081/sessions/google", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ token })
    });

    const data = await res.json();
    console.log("Respuesta backend:", data);

    if (data.token) {
      localStorage.setItem("token", data.token);
    }
  };

  return (
    <GoogleLogin
      onSuccess={handleSuccess}
      onError={() => console.log("Error login Google")}
    />
  );
}