import Layout from "@/components/Layout";
import config from '../../config.json'

export default function Oppijanumerorekisteri() {

    return (
        <Layout>
            <button style={{ width: 64, height: 32 }} onClick={() => {
                fetch(`${config.apiUrl}/api/test/onr`)
                    .then((data) => {
                        console.log("got response: ", data)
                    })
            }}>Test</button>
        </Layout>
    )
}