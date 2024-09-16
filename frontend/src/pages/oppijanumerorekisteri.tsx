import Layout from "@/components/Layout";
import config from '../../config.json'

export default function Oppijanumerorekisteri() {

    return (
        <Layout>
            <button style={{ width: 64, height: 32 }} onClick={() => {
                const url = `${config.apiUrl}/api/test/onr`
                console.log('calling url ', url)
                fetch(url)
                    .then((data) => {
                        console.log("got response: ", data)
                    })
                    .catch(err => {
                        console.log('received error', err)
                    })
            }}>Test</button>
        </Layout>
    )
}