import styles from '../styles/Home.module.css'
import {useState} from "react";
import {clsx} from 'clsx';
import Message from '../components/Message';

export default function Home() {
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState({
    message: '',
    error: false
  });

  async function load() {
    setLoading(true);

    function showMessage(message: string, error: boolean = false) {
      setStatus({
        message,
        error
      });
      setTimeout(() => setStatus({message: '', error: false}), 3000);
    }

    await fetch('https://unplgg.com/reallocate', {
      method: 'POST'
    })
      .then((response) => {
        if (response.status === 403) {
          throw new Error(response.statusText);
        }
      })
      .then(() => showMessage("Rotation completed!"))
      .catch((error) => showMessage(error.message, true))
      .finally(() => setTimeout(() => {
        setLoading(false);
      }, 1000));
  }

  return <div className={styles.main}>
    <div className={styles.relative}>
      <h1>Welcome to <span className={styles.gold}>Rate</span>Discovery!</h1>
      <button type="button" onClick={load} className={clsx({
        [styles.btnNormal]: !loading,
        [styles.btnLoading]: loading
      })}>{!loading ? 'Rotate the portfolio!' : 'In progress, please wait...'}</button>
      {status.message ? <Message status={status} /> : null}
    </div>
  </div>;
}
