import LoginMicrosoft from "./components/LoginMicrosoft";
import LoginGoogle from "./components/LoginGoogle";

function App() {
  return (
    <div>
      <h1>Login Sistema de Buses</h1>

      <LoginMicrosoft />

      <br />

      <LoginGoogle />
    </div>
  );
}

export default App;