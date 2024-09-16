import {ReactNode} from "react";
import Head from "next/head"
import Image from "next/image"
import localFont from "next/font/local"
import styles from "@/styles/Home.module.css"

const geistSans = localFont({
    src: "./../pages/fonts/GeistVF.woff",
    variable: "--font-geist-sans",
    weight: "100 900",
})
const geistMono = localFont({
    src: "./../pages/fonts/GeistMonoVF.woff",
    variable: "--font-geist-mono",
    weight: "100 900",
})

export default function Layout({ children }: { children: ReactNode }) {
    return (
        <>
            <Head>
                <title>Kielitutkintorekisteri</title>
                <meta name="description" content="Kielitutkintorekisteri" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <link rel="icon" href="/favicon.ico" />
            </Head>
            <div
                className={`${styles.page} ${geistSans.variable} ${geistMono.variable}`}
            >
                <main className={styles.main}>
                    {children}
                </main>
                <footer className={styles.footer}>
                    <a
                        href="https://www.oph.fi/"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        <Image
                            aria-hidden
                            src="/logo.svg"
                            alt="File icon"
                            width={16}
                            height={16}
                        />
                        Opetushallitus
                    </a>
                </footer>
            </div>
        </>
    )
}
