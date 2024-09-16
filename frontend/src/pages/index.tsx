import Image from "next/image"
import styles from "@/styles/Home.module.css"
import Layout from "@/components/Layout";

export default function Home() {
  return (
  <Layout>
    <Image
        className={styles.logo}
        src="/OPH_image_1.jpg"
        alt="Opetushallitus logo"
        width={42}
        height={42}
        priority
    />
    <ol>
      <li>OPH - Kielitutkintorekisteri</li>
    </ol>
  </Layout>
  )
}
