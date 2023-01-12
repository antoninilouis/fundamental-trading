import {Inter} from '@next/font/google'
import styles from '../styles/Home.module.css'
import {useState} from "react";
import {clsx} from 'clsx';

const inter = Inter({ subsets: ['latin'] })

export default function Home() {
  const [loading, setLoading] = useState(false);

  async function load() {
    setLoading(true);
    await fetch('https://fundamental-trading.eu-west-1.elasticbeanstalk.com/reallocate', {
      method: 'POST'
    })
        .then((response) => {
          if (response.status === 403) {
            throw new Error(response.statusText);
          }
        })
        .then(() => success())
        .catch((error) => alert(error))
        .finally(() => setTimeout(() => {
          setLoading(false);
        }, 1000));
  }

  function success() {
    alert("Success!");
  }

  return <div className={styles.main}>
    <h1>Welcome to RateDiscovery!</h1>
    <button type="button" onClick={load} className={clsx({
      [styles.btnNormal]: !loading,
      [styles.btnLoading]: loading
    })}>{!loading ? 'Rotate the portfolio!' : 'In progress, please wait...'}</button>
  </div>;
}
