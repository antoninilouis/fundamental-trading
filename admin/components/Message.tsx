import React from 'react'

import styles from '../styles/Message.module.css'

const Message = (props : {status: {message: string, error: boolean}}) => {
  return (
    <div className={styles['container']}>
      <div className={styles['container1']}>
        <span className={styles['text']}>{props.status.message}</span>
      </div>
    </div>
  )
}

export default Message
